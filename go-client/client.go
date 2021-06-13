package kevago

import (
	"github.com/tuhuynh27/keva/go-client/pool"
)

// 	"context"
// 	"net"

// 	"golang.org/x/net/context"

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
