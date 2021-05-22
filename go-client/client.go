package kevago

import (
	"fmt"
	"time"

	"github.com/duongcongtoai/kevago/pool"
)

// 	"context"
// 	"net"

// 	"golang.org/x/net/context"

type Config struct {
	Pool pool.Options
}

type Client struct {
	pool  *pool.ConnPool
	cmder commander
	// conn net.Conn //TODO: connection pool
	// ctx    context.Context
	// cancel context.CancelFunc
}

func NewClient(c Config) (*Client, error) {
	p, err := pool.NewConnPool(c.Pool)
	if err != nil {
		return nil, err
	}

	return &Client{
		pool:  p,
		cmder: globalCmd,
	}, nil
}

func (c *Client) Close() error {
	c.pool.Close()
	return nil
}

// func NewClient(c Config) (*Client, error) {
// 	conn, err := net.Dial("tcp", c.Endpoints[0])
// 	if err != nil {
// 		return nil, err
// 	}

// 	cl := &Client{conn: conn}
// 	ctx, cancel := context.WithCancel(context.Background())
// 	go cl.readLoop(ctx)
// 	go cl.writeLoop(ctx)
// 	cl.cancel = cancel
// 	cl.ctx = ctx
// 	return cl, nil
// }

func (c *Client) connectionIntercept(f func(*pool.Conn) error) error {
	conn, err := c.pool.Get()
	defer c.pool.Put(conn)
	if err != nil {
		return err
	}
	return f(conn)
}

//TODO: remove boiler plating code, reuse code according to input/output type
func (c *Client) Get(key string) (string, error) {
	comd := &getCmd{
		input: []string{key},
	}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})
	if err != nil {
		return "", err
	}
	return comd.result, nil
}

func (c *Client) Set(key string, value string) (string, error) {
	comd := &setCmd{
		input: []string{key, value},
	}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})
	if err != nil {
		return "", err
	}
	return comd.result, nil
}

func (c *Client) Delete(key string) (string, error) {
	comd := &delCmd{
		input: []string{key},
	}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})
	if err != nil {
		return "", err
	}
	return comd.result, nil
}

func (c *Client) Ping() error {
	comd := &pingCmd{}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})
	if err != nil {
		return err
	}
	return nil
}

func (c *Client) Info() (string, error) {
	comd := &infoCmd{}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})

	if err != nil {
		return "", err
	}
	return comd.result, nil
}
func (c *Client) Expire(key string,d time.Duration) (string, error) {
	comd := &expireCmd{
		input: []string{fmt.Sprintf("%s %d",key, d.Milliseconds())},
	}
	err := c.connectionIntercept(func(conn *pool.Conn) error {
		return c.cmder.execute(conn, comd)
	})

	if err != nil {
		return "", err
	}
	return comd.result, nil
}
