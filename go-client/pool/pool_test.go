package pool

import (
	"context"
	"net"
	"sync"
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

func TestConnReaper(t *testing.T) {
	const idleTimeout = 5 * time.Second
	const maxAge = time.Hour
	const poolSize = 100
	const minIdle = 10
	closedConnL := new(sync.Mutex)
	var closedConns []*Conn
	var connPool *ConnPool
	assertConnsReaperWork := func(t *testing.T) {
		initConnPool, err := NewConnPool(Options{
			Dialer:             dummyDialer,
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
		connPool = initConnPool
		assert.NoError(t, err)
		eventually(t, func() bool {
			return connPool.TotalIdleConns() == minIdle
		})

		var temp []*Conn = nil

		for i := 0; i < minIdle-3; i++ {
			con, err := connPool.Get()
			assert.NoError(t, err)
			temp = append(temp, con)
		}
		//wait for min idle to be ensured
		eventually(t, func() bool {
			return connPool.TotalIdleConns() == minIdle
		})

		//make all idle connection staled
		time.Sleep(idleTimeout)
		for _, item := range temp {
			item.SetLastUsed(time.Now().Add(2 * idleTimeout)) //make conn idle time out
			connPool.Put(item)
		}

		//reap staled connections
		connPool.reapStaleConns()

		assert.Equal(t, minIdle, len(closedConns))
	}

	assertConnsReaperWork(t)
}
