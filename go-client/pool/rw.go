package pool

import (
	"github.com/duongcongtoai/kevago/proto"
)

//TODO
func (c *Conn) WriteIntercept(f func(w *proto.Writer) error) error {
	return f(c.w)
}

//TODO
func (c *Conn) ReadIntercept(f func(w *proto.Reader) error) error {
	return f(c.r)
}
