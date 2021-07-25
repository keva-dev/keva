package kevago

import (
	"context"
	"net"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func setupDefault(t *testing.T) *Client {
	popt := Options{
		PoolTimeout: time.Second,
		PoolSize:    20,
		MinIdleConn: 5,
		Address:     "localhost:6767",
		Dialer: func(ctx context.Context, addr string) (net.Conn, error) {
			conn, err := net.Dial("tcp", addr)
			if err != nil {
				return nil, err
			}
			return conn, err
		},
		IdleTimeout:        time.Minute * 5,
		MaxConnAge:         time.Minute * 10,
		IdleCheckFrequency: time.Minute * 5,
	}
	cl, err := NewClient(ClientOptions{
		Pool: popt,
	})
	if err != nil {
		t.FailNow()
	}
	return cl
}

func TestCmd_CRUD(t *testing.T) {
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

func TestCmd_Expire(t *testing.T) {
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

func TestCmd_Ping(t *testing.T) {
	cl := setupDefault(t)
	defer cl.Close()
	err := cl.Ping()
	assert.NoError(t, err)

}

func TestCmd_Info(t *testing.T) {
	cl := setupDefault(t)
	defer cl.Close()
	_, err := cl.Info()
	assert.NoError(t, err)
}
