package sentinel

import (
	"bytes"
	"fmt"
	"strconv"
	"sync"
	"time"

	"github.com/google/uuid"
)

// ToyKeva simulator used for testing purpose
type ToyKeva struct {
	host string
	port string

	mu         *sync.Mutex
	alive      bool
	diedAt     *time.Time
	role       string
	id         string
	slaves     []*ToyKeva
	subs       map[string]chan string //key by fake sessionid
	*slaveInfo                        // only set if current keva is a slave
}

func (keva *ToyKeva) info() string {
	keva.mu.Lock()
	var ret = bytes.Buffer{}
	ret.WriteString(fmt.Sprintf("role:%s\n", keva.role))
	ret.WriteString(fmt.Sprintf("run_id:%s\n", keva.id))
	slaves := keva.slaves
	keva.mu.Unlock()
	for idx := range slaves {
		sl := keva.slaves[idx]
		sl.mu.Lock()
		state := "online"
		if !sl.alive {
			state = "offline"
		}
		ret.WriteString(fmt.Sprintf("slave%d:ip=%s,port=%s,state=%s,offset=%d,lag=%d\n",
			idx, sl.host, sl.port, state, sl.offset, sl.lag,
		))
		sl.mu.Unlock()
	}
	keva.mu.Lock()
	defer keva.mu.Unlock()
	if keva.role == "slave" {
		ret.WriteString(fmt.Sprintf("master_host:%s\n", keva.masterHost))
		ret.WriteString(fmt.Sprintf("master_port:%s\n", keva.masterPort))
		if !keva.master.isAlive() {
			ret.WriteString(fmt.Sprintf("master_link_down_since_seconds:%d\n", int(keva.master.diedSince().Seconds())))
			ret.WriteString("master_link_status:down\n")
		} else {
			ret.WriteString("master_link_down_since_seconds:-1\n")
			ret.WriteString("master_link_status:up\n")
		}
		ret.WriteString(fmt.Sprintf("slave_repl_offset:%d\n", keva.offset))
		ret.WriteString(fmt.Sprintf("slave_priority:%d\n", keva.priority))
	}
	return ret.String()
}

func (sl *slaveInfo) String() string {
	return fmt.Sprintf("offset: %d, priority: %d", sl.offset, sl.priority)
}

type slaveInfo struct {
	// runID  string

	masterHost string
	masterPort string
	offset     int
	lag        int
	priority   int
	master     *ToyKeva
}

func (keva *ToyKeva) diedSince() time.Duration {
	keva.mu.Lock()
	defer keva.mu.Unlock()
	return time.Since(*keva.diedAt)
}

func (keva *ToyKeva) kill() {
	keva.mu.Lock()
	defer keva.mu.Unlock()
	keva.alive = false
	now := time.Now()
	keva.diedAt = &now
}

func (keva *ToyKeva) isAlive() bool {
	keva.mu.Lock()
	defer keva.mu.Unlock()
	return keva.alive
}

type toyClient struct {
	link      *ToyKeva
	connected bool
	mu        *sync.Mutex
}

func NewToyKeva(host, port string) *ToyKeva {
	return &ToyKeva{
		id:    uuid.NewString(),
		mu:    &sync.Mutex{},
		subs:  map[string]chan string{},
		alive: true,
		host:  host,
		port:  port,
	}
}

func (keva *ToyKeva) withSlaves(num int) map[string]*ToyKeva {
	if keva.role != "master" {
		panic("cannot assign slaves to non master keva")
	}
	slaves := []*ToyKeva{}
	slaveMap := map[string]*ToyKeva{}

	for i := 0; i < num; i++ {
		newSlave := NewToyKeva("localhost", strconv.Itoa(i)) // fake port, toy master does not call toy slave through network call
		newSlave.turnToSlave()
		newSlave.host = "localhost"
		newSlave.slaveInfo = &slaveInfo{
			masterHost: keva.host,
			masterPort: keva.port,
			priority:   1,
			offset:     0,
			lag:        0, // TODO: don't understand what it means
			master:     keva,
		}
		addr := fmt.Sprintf("%s:%s", newSlave.host, newSlave.port)
		slaves = append(slaves, newSlave)
		slaveMap[addr] = newSlave
	}
	keva.slaves = slaves

	return slaveMap
}

func (keva *ToyKeva) turnToSlave() {
	keva.role = "slave"
	keva.alive = true
}

func (keva *ToyKeva) turnToMaster() {
	keva.role = "master"
	keva.alive = true
}

func NewToyKevaClient(keva *ToyKeva) *toyClient {
	return &toyClient{
		link:      keva,
		mu:        &sync.Mutex{},
		connected: true,
	}
}

func (cl *toyClient) Info() (string, error) {
	return cl.link.info(), nil
}

func (cl *toyClient) disconnect() {
	cl.mu.Lock()
	defer cl.mu.Unlock()
	cl.connected = false
}

// simulate network partition
func (cl *toyClient) isConnected() bool {
	cl.mu.Lock()
	defer cl.mu.Unlock()
	return cl.connected
}

func (cl *toyClient) Ping() (string, error) {
	if !cl.link.isAlive() || !cl.isConnected() {
		return "", fmt.Errorf("dead keva")
	}
	return "pong", nil
}

type toyHelloChan struct {
	root      *ToyKeva
	subChan   chan string
	sessionID string //
}

func (c *toyHelloChan) Publish(toBroadcast string) error {
	c.root.mu.Lock()
	for sessionID, sub := range c.root.subs {
		if sessionID == c.sessionID {
			continue
		}
		sub <- toBroadcast
	}
	c.root.mu.Unlock()
	return nil
}

func (c *toyHelloChan) Receive() (string, error) {
	newMsg := <-c.subChan
	return newMsg, nil
}

func (c *toyHelloChan) Close() error {
	c.root.mu.Lock()
	delete(c.root.subs, c.sessionID)
	c.root.mu.Unlock()
	close(c.subChan)
	return nil
}

func (cl *toyClient) SubscribeHelloChan() HelloChan {
	newChan := &toyHelloChan{
		root:      cl.link,
		subChan:   make(chan string, 1),
		sessionID: uuid.NewString(),
	}
	cl.link.mu.Lock()
	cl.link.subs[newChan.sessionID] = newChan.subChan
	cl.link.mu.Unlock()
	return newChan
}

// func (cl *toyClient) ExchangeSentinel(intro Intro) (ExchangeSentinelResponse, error) {
// 	cl.link.mu.Lock()
// 	var ret []SentinelIntroResponse
// 	for _, s := range cl.link.sentinels {
// 		ret = append(ret, SentinelIntroResponse{
// 			Addr:        s.addr,
// 			Port:        s.port,
// 			RunID:       s.runID,
// 			MasterName:  s.masterName,
// 			MasterPort:  s.masterPort,
// 			MasterAddr:  s.masterAddr,
// 			Epoch:       s.epoch,
// 			MasterEpoch: s.masterEpoch,
// 		})
// 	}
// 	cl.link.mu.Unlock()

// 	cl.link.addSentinel(intro)
// 	return ExchangeSentinelResponse{Sentinels: ret}, nil
// }
