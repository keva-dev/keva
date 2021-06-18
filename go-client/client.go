package kevago

import (
	"context"
	"net"
	"time"

	"github.com/tuhuynh27/keva/go-client/pool"
)

var (
	DefaultAddress = "localhost:6767"
)

type ClientOptions struct {
	Pool pool.Options
}

type Client struct {
	pool   *pool.ConnPool
	cmdMap commandMap
	commandAdaptor
	// conn net.Conn //TODO: connection pool
	// ctx    context.Context
	// cancel context.CancelFunc
}

func NewDefaultClient() (*Client, error) {
	poolOpt := pool.Options{
		Address:     DefaultAddress,
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
	}
	return NewClient(ClientOptions{
		Pool: poolOpt,
	})
}

func NewClient(c ClientOptions) (*Client, error) {
	p, err := pool.NewConnPool(c.Pool)
	if err != nil {
		return nil, err
	}

	client := &Client{
		pool:   p,
		cmdMap: globalCmds,
	}
	client.commandAdaptor = client.cmdWithPool
	return client, nil
}

func (c *Client) cmdWithPool(cmd Cmd) error {
	conn, err := c.pool.Get()
	if err != nil {
		return err
	}
	defer c.pool.Put(conn)
	return globalCmds.execute(conn, cmd)
}

func (c *Client) Close() error {
	c.pool.Close()
	return nil
}
