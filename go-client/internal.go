package kevago

import (
	"bufio"
	"net"
)

type InternalClient struct {
	cmdMap internalCmdMap
	internalCommandAdaptor
	conn *internalConn
	addr string
}

type internalConn struct {
	netConn net.Conn
	r       *bufio.Reader
	w       *bufio.Writer
}

func NewInternalClient(addr string) (*InternalClient, error) {
	conn, err := net.Dial("tcp", addr)
	if err != nil {
		return nil, err
	}
	cl := &InternalClient{
		cmdMap: internalcmds,
		conn:   &internalConn{netConn: conn},
		addr:   addr,
	}
	cl.internalCommandAdaptor = cl.process
	return cl, nil
}

func (c *InternalClient) process(cmd internalCmd) error {
	return c.cmdMap.execute(c.conn, cmd)
}

type internalCommandAdaptor func(internalCmd) error

func (c internalCommandAdaptor) Info() (string, error) {
	ret := internalInfoCmd{}
	c(&ret)
	return ret.ret, nil
}

type internalInfoCmd struct {
	ret string
}

func (i *internalInfoCmd) Name() string {
	return "info"
}
func (i *internalInfoCmd) Args() []string { return nil }

func (i *internalInfoCmd) ReadResult(r *bufio.Reader) error {
	i.ret = "dummy info"
	return nil
}
