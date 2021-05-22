package pool

import (
	"context"
	"net"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func dummyDialer(ctx context.Context) (net.Conn, error) {
	return &net.TCPConn{}, nil
}

func eventually(t *testing.T, f func() bool) {
	assert.Eventually(t, f, time.Second, time.Millisecond*10)
}

func TestMinIdleConns(t *testing.T) {
	const poolSize = 100
	createPoolFunc := func(minIdleConns int) *ConnPool {
		connPool, err := NewConnPool(Options{
			Dialer:             dummyDialer,
			PoolSize:           poolSize,
			MinIdleConn:        minIdleConns,
			PoolTimeout:        100 * time.Millisecond,
			IdleTimeout:        -1,
			IdleCheckFrequency: -1,
		})
		assert.NoError(t, err)
		eventually(t, func() bool {
			return connPool.TotalConns() == minIdleConns
		})
		eventually(t, func() bool {
			return connPool.TotalIdleConns() == minIdleConns
		})
		return connPool
	}

	assertPoolGetPut := func(t *testing.T, minIdleConn int) {
		connPool := createPoolFunc(minIdleConn)
		defer connPool.Close()
		assert.Equal(t, minIdleConn, connPool.TotalConns())
		assert.Equal(t, minIdleConn, connPool.TotalIdleConns())
		t.Run("Pool functionality", func(t *testing.T) {
			var cn *Conn
			incn, err := connPool.Get()
			assert.NoError(t, err)
			cn = incn

			//wait for min idle to be ensured
			eventually(t, func() bool {
				return connPool.TotalIdleConns() == minIdleConn
			})
			// assert.Equal(t, minIdleConn, connPool.TotalIdleConns())
			t.Run("Put back conn", func(t *testing.T) {
				connPool.Put(cn)
				assert.Equal(t, minIdleConn+1, connPool.TotalIdleConns())

			})
		})
	}
	t.Run("MinIdleConns = 1", func(t *testing.T) {
		assertPoolGetPut(t, 1)
	})
	t.Run("MinIdleConns = 32", func(t *testing.T) {
		assertPoolGetPut(t, 32)
	})
}

func TestConnectionReaper(t *testing.T) {
	const idleTimeout = 5 * time.Second
	const maxAge = time.Hour
	const poolSize = 100
	const minIdle = 10
	closedConnL := new(sync.Mutex)

	reapedConns := map[string]*Conn{}
	connPool, err := NewConnPool(Options{
		Dialer:             dummyDialer,
		PoolSize:           poolSize,
		IdleTimeout:        idleTimeout,
		MinIdleConn:        minIdle,
		MaxConnAge:         maxAge,
		PoolTimeout:        time.Second,
		IdleCheckFrequency: time.Hour,
		OnConnClosed: func(cn *Conn) error {
			closedConnL.Lock()
			reapedConns[cn.id] = cn
			closedConnL.Unlock()
			return nil
		},
	})
	assert.NoError(t, err)
	eventually(t, func() bool {
		return connPool.TotalIdleConns() == minIdle
	})

	var remainedConns []*Conn = nil

	//get all connections, force creating new idle connections
	for i := 0; i < minIdle; i++ {
		con, err := connPool.Get()
		assert.NoError(t, err)
		remainedConns = append(remainedConns, con)
	}

	//wait for min idle to be ensured
	eventually(t, func() bool {
		return connPool.TotalIdleConns() == minIdle
	})
	expectedToBeReaped := []string{}
	connPool.connsMu.Lock()
	for _, cn := range connPool.idleConns {
		expectedToBeReaped = append(expectedToBeReaped, cn.id)
	}
	connPool.connsMu.Unlock()

	//make all idle connection staled
	time.Sleep(idleTimeout)
	for _, item := range remainedConns {
		item.SetLastUsed(time.Now().Add(2 * idleTimeout)) //ensure they won't be timeout
		connPool.Put(item)
	}

	//reap staled connections
	connPool.reapStaleConns()

	assert.Equal(t, len(expectedToBeReaped), len(reapedConns))
	for _, id := range expectedToBeReaped {
		_, exist := reapedConns[id]
		assert.True(t, exist)
	}

}

func TestPoolGetNotReturnStaledConnection(t *testing.T) {
	const idleTimeout = 5 * time.Minute
	const maxAge = time.Hour
	const poolSize = 100
	const minIdle = 10
	closedConnL := new(sync.Mutex)
	var reapedConns []*Conn
	connPool, err := NewConnPool(Options{
		Dialer:             dummyDialer,
		PoolSize:           poolSize,
		IdleTimeout:        idleTimeout,
		MinIdleConn:        minIdle,
		MaxConnAge:         maxAge,
		PoolTimeout:        time.Second,
		IdleCheckFrequency: time.Hour,
		OnConnClosed: func(cn *Conn) error {
			closedConnL.Lock()
			reapedConns = append(reapedConns, cn)
			closedConnL.Unlock()
			return nil
		},
	})
	assert.NoError(t, err)
	eventually(t, func() bool {
		return connPool.TotalIdleConns() == minIdle
	})
	connPool.connsMu.Lock()
	nextPoppedConn := connPool.idleConns[len(connPool.idleConns)-1]
	id := nextPoppedConn.id
	expectedId := connPool.idleConns[len(connPool.idleConns)-2].id
	//make connection staled
	nextPoppedConn.SetLastUsed(time.Now().Add(-idleTimeout))
	connPool.connsMu.Unlock()
	conn, err := connPool.Get()
	assert.NoError(t, err)
	assert.NotEqual(t, id, conn.id)
	assert.Equal(t, expectedId, conn.id)
}

func makeDelayedDialer(delayAt int) (func(context.Context) (net.Conn, error), chan struct{}) {
	counter := int32(0)
	signalReached := make(chan struct{})
	return func(ctx context.Context) (net.Conn, error) {
		if atomic.AddInt32(&counter, 1) == int32(delayAt) {
			signalReached <- struct{}{}
			<-signalReached
			return &net.TCPConn{}, nil
		}
		return &net.TCPConn{}, nil
	}, signalReached
}

func TestUnmanagedConnRemovedOnPutback(t *testing.T) {
	const idleTimeout = 5 * time.Minute
	const maxAge = time.Hour
	const poolSize = 11 // make semaphore extra 1 connection
	const minIdle = 10
	closedConnL := new(sync.Mutex)
	var closedConns []*Conn
	delayedDialer, limitReached := makeDelayedDialer(11)
	connPool, err := NewConnPool(Options{
		Dialer:             delayedDialer, //11st connection will take long to establish
		PoolSize:           poolSize,
		IdleTimeout:        idleTimeout,
		MinIdleConn:        minIdle,
		MaxConnAge:         maxAge,
		PoolTimeout:        time.Second,
		IdleCheckFrequency: time.Hour,
		OnConnClosed: func(cn *Conn) error {
			closedConnL.Lock()
			closedConns = append(closedConns, cn)
			closedConnL.Unlock()
			return nil
		},
	})
	assert.NoError(t, err)
	eventually(t, func() bool {
		return connPool.TotalIdleConns() == minIdle
	})
	for i := 0; i < minIdle; i++ {
		_, err := connPool.Get()
		assert.NoError(t, err)
	}
	//dialing new connection is being delayed, total idle is 0 now
	<-limitReached
	unmanagedConn, err := connPool.Get()
	limitReached <- struct{}{}
	assert.NoError(t, err)
	assert.False(t, unmanagedConn.IsManaged())
	connPool.Put(unmanagedConn)
	assert.Equal(t, 1, len(closedConns))
	assert.Equal(t, unmanagedConn.id, closedConns[0].id)
}

func TestRemoveConn(t *testing.T) {
	const idleTimeout = 5 * time.Minute
	const maxAge = time.Hour
	const poolSize = 11 // make semaphore extra 1 connection
	const minIdle = 10
	closedConnL := new(sync.Mutex)
	var closedConns []*Conn
	connPool, err := NewConnPool(Options{
		Dialer:             dummyDialer, //11st connection will take long to establish
		PoolSize:           poolSize,
		IdleTimeout:        idleTimeout,
		MinIdleConn:        minIdle,
		MaxConnAge:         maxAge,
		PoolTimeout:        time.Second,
		IdleCheckFrequency: time.Hour,
		OnConnClosed: func(cn *Conn) error {
			closedConnL.Lock()
			closedConns = append(closedConns, cn)
			closedConnL.Unlock()
			return nil
		},
	})
	assert.NoError(t, err)
	eventually(t, func() bool {
		return connPool.TotalConns() == minIdle
	})
	cn, err := connPool.Get()
	assert.NoError(t, err)
	connPool.RemoveConn(cn)
	assert.Equal(t, 1, len(closedConns))
	//connection is closed
	assert.Equal(t, closedConns[0].id, cn.id)
	assert.Equal(t, cn.id, closedConns[0].id)

	connPool.connsMu.Lock()
	defer connPool.connsMu.Unlock()
	for _, con := range connPool.idleConns {
		assert.NotEqual(t, cn.id, con.id)
	}
}
