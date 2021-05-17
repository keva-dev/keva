package kevago

import (
	"context"
	"net"
	"testing"
	"time"

	"github.com/duongcongtoai/kevago/pool"
	"github.com/stretchr/testify/assert"
)

func setupDefault(t *testing.T) *Client {
	popt := pool.Options{
		PoolTimeout: time.Second,
		PoolSize:    20,
		MinIdleConn: 5,
		Dialer: func(ctx context.Context) (net.Conn, error) {
			conn, err := net.Dial("tcp", "localhost:6767")
			if err != nil {
				return nil, err
			}
			return conn, err
		},
		IdleTimeout:        time.Minute * 5,
		MaxConnAge:         time.Minute * 10,
		IdleCheckFrequency: time.Minute * 5,
	}
	cl, err := NewClient(Config{
		Pool: popt,
	})
	if err != nil {
		t.FailNow()
	}
	return cl
}

func TestCRUD(t *testing.T) {
	cl := setupDefault(t)
	defer cl.Close()

	ret, err := cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

	ret, err = cl.Set("key1", "value1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "value1", ret)

	ret, err = cl.Delete("key1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)
}

func TestExpire(t *testing.T) {
	cl := setupDefault(t)
	defer cl.Close()

	ret, err := cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

	ret, err = cl.Set("key2", "value1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "value1", ret)

	ret, err = cl.Expire("key2", 2000*time.Millisecond)
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)
	time.Sleep(2000 * time.Millisecond)

	ret, err = cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

}

func TestPing(t *testing.T) {

	cl := setupDefault(t)
	defer cl.Close()
	err := cl.Ping()
	assert.NoError(t, err)

}
