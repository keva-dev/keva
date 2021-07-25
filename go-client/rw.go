package kevago

import (
	"github.com/tuhuynh27/keva/go-client/proto"
)

//TODO
func (c *Conn) WriteIntercept(f func(w *proto.Writer) error) error {
	return f(c.w)
}

//TODO
func (c *Conn) ReadIntercept(f func(w *proto.Reader) error) error {
	return f(c.r)
}
