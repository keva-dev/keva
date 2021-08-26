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
	"golang.org/x/sync/errgroup"
)

var (
	defaultMasterAddr = "localhost:6767"
)

type testSuite struct {
	instances []*Sentinel
	links     []*toyClient
	conf      Config
	master    *ToyKeva
}

func (t *testSuite) CleanUp() {
	for _, instance := range t.instances {
		instance.Shutdown()
	}
}

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
	sentinels := []*Sentinel{}
	links := make([]*toyClient, numInstances)
	testLock := &sync.Mutex{}
	master.turnToMaster()
	basePort := 2000
	for i := 0; i < numInstances; i++ {
		s, err := NewFromConfig(conf)
		s.conf.Port = strconv.Itoa(basePort + i)
		assert.NoError(t, err)

		s.clientFactory = func(addr string) (internalClient, error) {
			cl := NewToyKevaClient(master)
			testLock.Lock()
			links[i] = cl
			testLock.Unlock()
			return cl, nil
		}
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
	return &testSuite{
		instances: sentinels,
		links:     links,
		conf:      conf,
		master:    master,
	}
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

func TestLeaderElection(t *testing.T) {
	// suite := setup(t,3,)
}
func eventually(t *testing.T, f func() bool, duration time.Duration) bool {
	return assert.Eventually(t, f, duration, 50*time.Millisecond)
}

func checkMasterState(t *testing.T, masterAddr string, s *Sentinel, state masterInstanceState) {
	s.mu.Lock()
	m := s.masterInstances[masterAddr]
	s.mu.Unlock()
	assert.Equal(t, state, m.getState())
}

func masterStateIs(masterAddr string, s *Sentinel, state masterInstanceState) bool {
	s.mu.Lock()
	m := s.masterInstances[masterAddr]
	s.mu.Unlock()
	return state == m.getState()
}
