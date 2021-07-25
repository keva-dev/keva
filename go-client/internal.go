package kevago

// type InternalClient struct {
// 	// pool   *ConnPool
// 	cmdMap commandMap
// 	commandAdaptor
// 	// conn net.Conn //TODO: connection pool
// 	// ctx    context.Context
// 	// cancel context.CancelFunc
// }

// func NewInternalClient(c ClientOptions) (*Client, error) {
// 	p, err := NewConnPool(c.Pool)
// 	if err != nil {
// 		return nil, err
// 	}

// 	client := &Client{
// 		pool:   p,
// 		cmdMap: globalCmds,
// 	}
// 	client.commandAdaptor = client.cmdWithPool
// 	return client, nil
// }

// func (c *Client) cmdWithPool(cmd Cmd) error {
// 	conn, err := c.Get()
// 	if err != nil {
// 		return err
// 	}
// 	defer c.Put(conn)
// 	return globalCmds.execute(conn, cmd)
// }
