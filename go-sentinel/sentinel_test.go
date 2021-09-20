package sentinel

import (
	"fmt"
	"path/filepath"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
	"golang.org/x/sync/errgroup"
)

var (
	stdLogger *zap.SugaredLogger
)

func init() {
	stdLogger = newLogger()
}

func customLogObserver() (*zap.SugaredLogger, *observer.ObservedLogs) {
	observedCore, recordedLogs := observer.New(zap.DebugLevel)
	normalCore := stdLogger.Desugar().Core()
	teeCore := zapcore.NewTee(observedCore, normalCore)
	built := zap.New(teeCore, zap.WithCaller(true)).Sugar()
	return built, recordedLogs
}

var (
	defaultMasterAddr = "localhost:6767"
)

type testSuite struct {
	mu            *sync.Mutex
	mapRunIDtoIdx map[string]int // code use runID as identifier, test suite use integer index, use this to remap
	mapIdxtoRunID map[int]string // code use runID as identifier, test suite use integer index, use this to remap
	instances     []*Sentinel
	links         []*toyClient
	conf          Config
	master        *ToyKeva
	slavesMap     map[string]*ToyKeva
	history
	logObservers []*observer.ObservedLogs
	t            *testing.T
}
type history struct {
	currentLeader      string
	currentTerm        int
	termsVote          map[int][]termInfo // key by term seq, val is array of each sentinels' term info
	termsLeader        map[int]string
	termsSelectedSlave map[int]string
	failOverStates     map[int]failOverState // capture current failoverstate of each instance
}
type termInfo struct {
	selfVote      string
	neighborVotes map[string]string // info about what a sentinel sees other sentinel voted
}

func (t *testSuite) CleanUp() {
	for _, instance := range t.instances {
		instance.Shutdown()
	}
}

// this function also pre spawn some goroutines to continuously update testSuite state according
// to event logs recorded from sentinels instance, test functions only need to assert those states
func setupWithCustomConfig(t *testing.T, numInstances int, customConf func(*Config)) *testSuite {
	file := filepath.Join("test", "config", "sentinel.yaml")
	viper.SetConfigType("yaml")
	viper.SetConfigFile(file)
	err := viper.ReadInConfig()
	assert.NoError(t, err)

	var conf Config
	err = viper.Unmarshal(&conf)
	assert.NoError(t, err)
	if customConf != nil {
		customConf(&conf)
	}

	master := NewToyKeva()
	master.turnToMaster()
	slaveMap := master.withSlaves(3)
	toySlaveFactory := func(sl *slaveInstance) error {
		toyInstance := slaveMap[sl.addr]
		sl.client = NewToyKevaClient(toyInstance)
		return nil
	}
	// make master remember slaves
	sentinels := []*Sentinel{}
	links := make([]*toyClient, numInstances)
	logObservers := make([]*observer.ObservedLogs, numInstances)
	testLock := &sync.Mutex{}
	basePort := 2000
	mapRunIDToIdx := map[string]int{}
	mapIdxToRunID := map[int]string{}
	for i := 0; i < numInstances; i++ {
		s, err := NewFromConfig(conf)
		s.conf.Port = strconv.Itoa(basePort + i)
		assert.NoError(t, err)
		mapRunIDToIdx[s.runID] = i
		mapIdxToRunID[i] = s.runID

		s.clientFactory = func(addr string) (internalClient, error) {
			cl := NewToyKevaClient(master)
			testLock.Lock()
			links[i] = cl
			testLock.Unlock()
			return cl, nil
		}

		s.slaveFactory = toySlaveFactory
		customLogger, observer := customLogObserver()
		logObservers[i] = observer
		s.logger = customLogger
		err = s.Start()
		assert.NoError(t, err)

		sentinels = append(sentinels, s)
		defer s.Shutdown()
	}
	// sleep for 2 second to ensure all sentinels have pubsub and recognized each other
	time.Sleep(2 * time.Second)

	for _, s := range sentinels {
		s.mu.Lock()
		masterI, ok := s.masterInstances[defaultMasterAddr]
		assert.True(t, ok)
		s.mu.Unlock()
		masterI.mu.Lock()
		assert.Equal(t, numInstances-1, len(masterI.sentinels))
		masterI.mu.Unlock()
	}
	suite := &testSuite{
		instances: sentinels,
		links:     links,
		mu:        testLock,
		conf:      conf,
		master:    master,
		slavesMap: slaveMap,
		history: history{
			termsVote:      map[int][]termInfo{},
			termsLeader:    map[int]string{},
			failOverStates: map[int]failOverState{},
		},
		mapRunIDtoIdx: mapRunIDToIdx,
		logObservers:  logObservers,
		t:             t,
	}
	for idx := range suite.logObservers {
		go suite.consumeLogs(idx, suite.logObservers[idx])
	}
	return suite
}

func (s *testSuite) consumeLogs(instanceIdx int, observer *observer.ObservedLogs) {
	for {
		logs := observer.TakeAll()
		for _, entry := range logs {
			switch entry.Message {
			case logEventBecameTermLeader:
				s.handleLogEventBecameTermLeader(instanceIdx, entry)
			case logEventVotedFor:
				s.handleLogEventSentinelVotedFor(instanceIdx, entry)
			case logEventNeighborVotedFor:
				s.handleLogEventNeighborVotedFor(instanceIdx, entry)
			case logEventFailoverStateChanged:
				s.handleLogEventFailoverStateChanged(instanceIdx, entry)
			case logEventSelectedSlave:
				s.handleLogEventSelectedSlave(instanceIdx, entry)
			}
		}
		time.Sleep(100 * time.Millisecond)
	}
	// all events that may change state of this suite from log stream appear here
}

func setup(t *testing.T, numInstances int) *testSuite {
	return setupWithCustomConfig(t, numInstances, nil)
}

func TestInit(t *testing.T) {
	t.Run("3 instances", func(t *testing.T) {
		s := setup(t, 3)
		s.CleanUp()
	})
	t.Run("5 instances", func(t *testing.T) {
		s := setup(t, 5)
		s.CleanUp()
	})

}

func TestSDown(t *testing.T) {
	checkSdown := func(t *testing.T, numSdown int, numInstances int) {
		suite := setupWithCustomConfig(t, numInstances, func(c *Config) {
			//important: adjust quorum to the strictest level = numInstances
			c.Masters[0].Quorum = numInstances
		})
		defer suite.CleanUp()

		for _, s := range suite.instances {
			s.mu.Lock()
			masterI, ok := s.masterInstances[defaultMasterAddr]
			assert.True(t, ok)
			s.mu.Unlock()
			masterI.mu.Lock()
			assert.Equal(t, numInstances-1, len(masterI.sentinels))
			masterI.mu.Unlock()
		}

		disconnecteds := suite.links[:numSdown]
		for _, link := range disconnecteds {
			link.disconnect()
		}
		// links[disconnectedIdx].disconnect()

		time.Sleep(suite.conf.Masters[0].DownAfter)
		// 1 more second for sure
		time.Sleep(1 * time.Second)

		// check if a given sentinel is in sdown state, and holds for a long time
		for i := 0; i < numSdown; i++ {
			checkMasterState(t, defaultMasterAddr, suite.instances[i], masterStateSubjDown)
		}
		// others still see master is up
		for i := numSdown; i < numInstances; i++ {
			checkMasterState(t, defaultMasterAddr, suite.instances[i], masterStateUp)
		}
	}
	t.Run("1 out of 3 subjectively down", func(t *testing.T) {
		checkSdown(t, 1, 3)
	})
	t.Run("2 out of 3 subjectively down", func(t *testing.T) {
		checkSdown(t, 2, 3)
	})
	t.Run("3 out of 5 subjectively down", func(t *testing.T) {
		checkSdown(t, 3, 5)
	})

}

func TestODown(t *testing.T) {
	testOdown := func(t *testing.T, numInstances int) {
		suite := setup(t, numInstances)

		for _, s := range suite.instances {
			s.mu.Lock()
			masterI, ok := s.masterInstances[defaultMasterAddr]
			assert.True(t, ok)
			s.mu.Unlock()
			masterI.mu.Lock()
			assert.Equal(t, numInstances-1, len(masterI.sentinels))
			masterI.mu.Unlock()
		}
		suite.master.kill()

		time.Sleep(suite.conf.Masters[0].DownAfter)

		// check if a given sentinel is in sdown state, and holds for a long time
		// others still see master is up
		gr := errgroup.Group{}
		for idx := range suite.instances {
			localSentinel := suite.instances[idx]
			gr.Go(func() error {
				met := eventually(t, func() bool {
					return masterStateIs(defaultMasterAddr, localSentinel, masterStateObjDown)
				}, 5*time.Second)
				if !met {
					return fmt.Errorf("sentinel %s did not recognize master as o down", localSentinel.listener.Addr())
				}
				return nil
			})
		}
		assert.NoError(t, gr.Wait())
	}
	t.Run("3 instances o down", func(t *testing.T) {
		testOdown(t, 3)
	})
	t.Run("5 instances o down", func(t *testing.T) {
		testOdown(t, 5)
	})
}
func (suite *testSuite) checkTermVoteOfSentinel(t *testing.T, sentinelIdx int, term int) {
	currentSentinel := suite.instances[sentinelIdx]
	reached := eventually(t, func() bool {
		suite.mu.Lock()
		defer suite.mu.Unlock()
		termInfo := suite.termsVote[term][sentinelIdx]
		return termInfo.selfVote != ""
	}, 10*time.Second, "sentinel %s never votes for any instance in term %d", currentSentinel.runID, term)
	if !reached {
		assert.FailNowf(suite.t, "sentinel did not vote for any instance", "")
	}

}

func (suite *testSuite) checkTermVoteOfSentinelNeighbor(t *testing.T, instanceIdx int, neighborID string, term int) {
	currentSentinel := suite.instances[instanceIdx]
	eventually(t, func() bool {
		suite.mu.Lock()
		defer suite.mu.Unlock()
		termInfo := suite.termsVote[term][instanceIdx]

		if termInfo.neighborVotes == nil {
			return false
		}
		vote := termInfo.neighborVotes[neighborID]
		return vote != ""
	}, 10*time.Second, "sentinel %s cannot get its neighbor's leader in term %d", currentSentinel.runID, term)
}

// - create a stream of logs, observe from stream and change status of test suite
// - assert function wait for the change of status only
func TestLeaderVoteNotConflict(t *testing.T) {
	assertion := func(t *testing.T, numInstances int) {
		suite := setupWithCustomConfig(t, numInstances, func(c *Config) {
			c.Masters[0].Quorum = numInstances/2 + 1 // force normal quorum
		})
		suite.master.kill()
		time.Sleep(suite.conf.Masters[0].DownAfter)

		// check if a given sentinel is in sdown state, and holds for a long time
		// others still see master is up
		gr := errgroup.Group{}
		suite.mu.Lock()
		suite.termsVote[1] = make([]termInfo, len(suite.instances))
		suite.mu.Unlock()
		for idx := range suite.instances {
			instanceIdx := idx
			gr.Go(func() error {
				suite.checkTermVoteOfSentinel(t, instanceIdx, 1)
				return nil
			})
		}
		gr.Wait()

		gr2 := errgroup.Group{}
		for idx := range suite.instances {
			localSentinel := suite.instances[idx]
			m := getSentinelMaster(defaultMasterAddr, localSentinel)
			m.mu.Lock()

			for sentinelIdx := range m.sentinels {
				si := m.sentinels[sentinelIdx]
				si.mu.Lock()
				neighborID := si.runID
				si.mu.Unlock()

				instanceIdx := idx

				gr2.Go(func() error {
					suite.checkTermVoteOfSentinelNeighbor(t, instanceIdx, neighborID, 1)
					return nil
				})
			}
			m.mu.Unlock()
		}
		gr2.Wait()
		suite.mu.Lock()
		defer suite.mu.Unlock()

		for idx := range suite.instances {
			thisInstanceHistory := suite.termsVote[1][idx]
			thisInstanceVote := thisInstanceHistory.selfVote

			thisInstanceID := suite.instances[idx].runID

			for idx2 := range suite.instances {
				if idx2 == idx {
					continue
				}
				neiborInstanceVote := suite.termsVote[1][idx2]
				if neiborInstanceVote.neighborVotes[thisInstanceID] != thisInstanceVote {
					assert.Failf(t, "conflict vote between instances",
						"instance %s records that instance %s voted for %s, but %s says it voted for %s",
						suite.instances[idx2].runID,
						thisInstanceID,
						neiborInstanceVote.neighborVotes[thisInstanceID],
						thisInstanceID,
						thisInstanceVote,
					)
				}
			}
		}
		// 1.for each instance, compare its vote with how other instances records its vote
		// 2.record each instance leader, find real leader of that term
		// 3.find that real leader and check if its failover state is something in selecting slave
	}
	t.Run("3 instances do not conflict", func(t *testing.T) {
		assertion(t, 3)
	})
}

func TestLeaderElection(t *testing.T) {
	assertion := func(t *testing.T, numInstances int) {
		suite := setupWithCustomConfig(t, numInstances, func(c *Config) {
			c.Masters[0].Quorum = numInstances/2 + 1 // force normal quorum
		})
		suite.master.kill()
		time.Sleep(suite.conf.Masters[0].DownAfter)
		//TODO: check more info of this recognized leader
		suite.checkClusterHasLeader()
	}
	t.Run("3 instances vote leader success", func(t *testing.T) {
		assertion(t, 3)
	})
}

func TestReconfSlave(t *testing.T) {
	assertion := func(t *testing.T, numInstances int, slaveCustomizer func(*testSuite) *ToyKeva) {
		suite := setupWithCustomConfig(t, numInstances, func(c *Config) {
			c.Masters[0].Quorum = numInstances/2 + 1 // force normal quorum
		})
		expectedChosenSlave := slaveCustomizer(suite)

		suite.master.kill()
		time.Sleep(suite.conf.Masters[0].DownAfter)
		//TODO: check more info of this recognized leader
		suite.checkClusterHasLeader()
		selectedSlave := suite.checkTermSelectedSlave(1)
		assert.Equal(t, expectedChosenSlave.id, selectedSlave)
	}
	t.Run("select slave by highest offset", func(t *testing.T) {
		assertion(t, 3, func(suite *testSuite) *ToyKeva {
			for idx := range suite.slavesMap {
				slave := suite.slavesMap[idx]
				slave.mu.Lock()
				slave.offset = 10
				slave.mu.Unlock()
				return slave
			}
			return nil
		})
	})
	t.Run("select slave by highest priority", func(t *testing.T) {
		assertion(t, 3, func(suite *testSuite) *ToyKeva {
			for idx := range suite.slavesMap {
				slave := suite.slavesMap[idx]
				slave.mu.Lock()
				slave.priority = 10
				slave.mu.Unlock()
				return slave
			}
			return nil
		})
	})
}
func (s *testSuite) checkTermSelectedSlave(term int) string {
	var ret string
	eventually(s.t, func() bool {
		s.mu.Lock()
		defer s.mu.Unlock()
		selected, exist := s.termsSelectedSlave[term]
		if exist && selected != "" {
			ret = selected
			return true
		}
		return false
	}, 10*time.Second)
	return ret
}

func (s *testSuite) checkClusterHasLeader() {
	eventually(s.t, func() bool {
		s.mu.Lock()
		defer s.mu.Unlock()
		return s.currentLeader != ""
	}, 10*time.Second)
}

func getSentinelMaster(masterAddr string, s *Sentinel) *masterInstance {
	s.mu.Lock()
	m := s.masterInstances[masterAddr]
	s.mu.Unlock()
	return m
}

func eventually(t *testing.T, f func() bool, duration time.Duration, msgAndArgs ...interface{}) bool {
	return assert.Eventually(t, f, duration, 50*time.Millisecond, msgAndArgs...)
}

func checkMasterState(t *testing.T, masterAddr string, s *Sentinel, state masterInstanceState) {
	assert.Equal(t, state, getSentinelMaster(masterAddr, s).getState())
}

func masterStateIs(masterAddr string, s *Sentinel, state masterInstanceState) bool {
	s.mu.Lock()
	m := s.masterInstances[masterAddr]
	s.mu.Unlock()
	return state == m.getState()
}
