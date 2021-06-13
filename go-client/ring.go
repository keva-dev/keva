package kevago

import (
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	"github.com/tuhuynh27/keva/go-client/pool"
)

type RingOptions struct {
	HealthCheckFrequency time.Duration
	Addresses            []string
	HashAlgorithm        HashAlgo
	ClientOpts           ClientOptions
}

type HashAlgo string

var (
	Rendezvous HashAlgo = "rendezvous"
	Jump       HashAlgo = "jump"
	Maglev     HashAlgo = "maglev"
)

type ConsistentHash interface {
	Lookup(key string) string
}

type Ring struct {
	opts                RingOptions
	consistentHash      ConsistentHash
	resetConsistentHash func(addrs []string) ConsistentHash
	allShards           map[string]*shardClient
	mu                  sync.RWMutex
	commandAdaptor
}

type shardClient struct {
	client    *Client
	downCount int32
}

func NewRing(opts RingOptions) error {
	r := &Ring{
		opts: opts,
		mu:   sync.RWMutex{},
	}
	switch opts.HashAlgorithm {
	case Rendezvous:
		r.resetConsistentHash = func(addrs []string) ConsistentHash {
			return newRendezvous(addrs)
		}
	default:
		return fmt.Errorf("unsupported hash algorithm: %s", opts.HashAlgorithm)
	}
	allShards := make(map[string]*shardClient)
	for _, addr := range opts.Addresses {
		client, err := NewClient(opts.ClientOpts)
		if err != nil {
			return fmt.Errorf("failed to create client for address %s: %s", addr, err)
		}
		allShards[addr] = &shardClient{
			downCount: 0,
			client:    client,
		}
	}
	r.allShards = allShards
	r.updateAliveShards()
	go r.healthCheckByInterval(opts.HealthCheckFrequency)
	r.commandAdaptor = r.cmdWithConsistentHash
	return nil
}

func (r *Ring) cmdWithConsistentHash(cmd Cmd) error {
	r.mu.RLock()
	addr := r.consistentHash.Lookup(cmd.Args()[0])
	correctShard, ok := r.allShards[addr]
	r.mu.RUnlock()
	if !ok {
		return fmt.Errorf("address after consistent hash %s does not exist", addr)
	}
	return correctShard.client.commandAdaptor(cmd)
}

func (r *Ring) getShards() []*shardClient {
	var ret []*shardClient
	r.mu.RLock()
	for _, s := range r.allShards {
		ret = append(ret, s)
	}
	r.mu.RUnlock()
	return ret
}

func (r *Ring) healthCheckByInterval(frequency time.Duration) {
	ticker := time.NewTicker(frequency)
	defer ticker.Stop()
	for range ticker.C {
		var shardsChanged bool

		for _, shard := range r.getShards() {
			err := shard.client.Ping()
			isUp := err == nil || err == pool.ErrPoolTimeout
			statusChanged := shard.updateStatus(isUp)
			if statusChanged {
				shardsChanged = true
			}
		}
		if shardsChanged {
			r.updateAliveShards()
		}
	}
}

func (r *Ring) updateAliveShards() {
	r.mu.RLock()
	shards := r.allShards
	r.mu.RUnlock()

	aliveShards := make([]string, 0, len(shards))

	for addr, shard := range shards {
		if !shard.isDown() {
			aliveShards = append(aliveShards, addr)
		}
	}
	hasher := r.resetConsistentHash(aliveShards)
	r.consistentHash = hasher

	r.mu.Lock()
	r.consistentHash = hasher
	r.mu.Unlock()
}

func (s *shardClient) isDown() bool {
	const threshold = 3
	return atomic.LoadInt32(&s.downCount) > threshold
}

func (s *shardClient) updateStatus(isUp bool) (statusChanged bool) {
	if isUp {
		isDown := s.isDown()
		atomic.StoreInt32(&s.downCount, 0)
		statusChanged = isDown
		return
	}
	isDownAlready := s.isDown()

	if isDownAlready {
		return false
	}

	atomic.AddInt32(&s.downCount, 1)
	statusChanged = s.isDown()
	return
}
