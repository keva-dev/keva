package sentinel

import (
	"bytes"
	"fmt"
	"sync"

	"github.com/google/uuid"
)

// ToyKeva simulator used for testing purpose
type ToyKeva struct {
	mu        *sync.Mutex
	alive     bool
	role      string
	id        string
	slaves    []toySlave
	sentinels map[string]toySentinel
	subs      map[string]chan string //key by fake sessionid
}

func (keva *ToyKeva) info() string {
	keva.mu.Lock()
	defer keva.mu.Unlock()
	var ret = bytes.Buffer{}
	ret.WriteString(fmt.Sprintf("role:%s\n", keva.role))
	ret.WriteString(fmt.Sprintf("run_id:%s\n", keva.id))
	for idx, sl := range keva.slaves {
		ret.WriteString(fmt.Sprintf("slave%d:ip=%s,port=%s,state=%s,offset=%d,lag=%d\n",
			idx, sl.addr, sl.state, sl.port, sl.offset, sl.lag,
		))
	}
	return ret.String()
}

type toySentinel struct {
	addr        string
	port        string
	runID       string
	epoch       int
	masterEpoch int
	masterName  string
	masterAddr  string
	masterPort  string
}

type toySlave struct {
	addr   string
	port   string
	state  string
	offset int
	lag    int
}

func (keva *ToyKeva) kill() {
	keva.mu.Lock()
	defer keva.mu.Unlock()
	keva.alive = false
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

func NewToyKeva() *ToyKeva {
	return &ToyKeva{
		mu:    &sync.Mutex{},
		subs:  map[string]chan string{},
		alive: true,
	}
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
