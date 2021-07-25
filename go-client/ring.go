package kevago

import (
	"context"
	"fmt"
	"net"
	"sync"
	"sync/atomic"
	"time"
)

type RingOptions struct {
	HealthCheckFrequency time.Duration
	Addresses            []string
	HashAlgorithm        HashAlgo

	ClientOptions RingClientOpt
}
type RingClientOpt struct {
	PoolTimeout        time.Duration
	PoolSize           int
	MinIdleConn        int
	Dialer             func(ctx context.Context, addr string) (net.Conn, error)
	IdleTimeout        time.Duration
	MaxConnAge         time.Duration
	IdleCheckFrequency time.Duration
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

func NewDefaultRing(addr []string) (*Ring, error) {
	defaultOpts := RingOptions{
		HealthCheckFrequency: 30 * time.Second,
		HashAlgorithm:        Rendezvous,
		ClientOptions: RingClientOpt{
			PoolTimeout: time.Second, // max time to wait to get new connection from pool
			PoolSize:    20,          // max number of connection can get from the pool
			MinIdleConn: 5,
			Dialer: func(ctx context.Context, addr string) (net.Conn, error) { //Must define dialer func
				conn, err := net.Dial("tcp", addr)
				if err != nil {
					return nil, err
				}
				return conn, err
			},
			IdleTimeout:        time.Minute * 5,  // if connection lives longer than 5 minutes, it is removable
			MaxConnAge:         time.Minute * 10, // all connections cannot live longer than this
			IdleCheckFrequency: time.Minute * 5,  // reap staled connections after 5 minutes
		},
	}
	defaultOpts.Addresses = addr
	return NewRing(defaultOpts)
}

// NewRing return a Ring behaves like a single client, with consistent hashing
func NewRing(opts RingOptions) (*Ring, error) {
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
		return nil, fmt.Errorf("unsupported hash algorithm: %s", opts.HashAlgorithm)
	}
	allShards := make(map[string]*shardClient)
	clientOpt := opts.ClientOptions
	for _, addr := range opts.Addresses {
		clientOpt := ClientOptions{
			Pool: Options{
				Address:            addr,
				PoolTimeout:        clientOpt.PoolTimeout,
				PoolSize:           clientOpt.PoolSize,
				MinIdleConn:        clientOpt.MinIdleConn,
				Dialer:             clientOpt.Dialer,
				IdleTimeout:        clientOpt.IdleTimeout,
				MaxConnAge:         clientOpt.MaxConnAge,
				IdleCheckFrequency: clientOpt.IdleCheckFrequency,
			},
		}
		client, err := NewClient(clientOpt)
		if err != nil {
			return nil, fmt.Errorf("failed to create client for address %s: %s", addr, err)
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
	return r, nil
}

func (r *Ring) Close() error {
	return nil
	//TODO
}

func (r *Ring) cmdWithConsistentHash(cmd Cmd) error {
	r.mu.RLock()
	args := cmd.Args()
	if len(args) == 0 {
		return fmt.Errorf("command with no argument is not supported by ring just yet")
	}
	addr := r.consistentHash.Lookup(args[0])
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
			isUp := err == nil || err == ErrPoolTimeout
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
