package kevago

import (
	"github.com/duongcongtoai/kevago/proto"
)

type expireCmd struct {
	input  []string
	result string
}

func (g *expireCmd) Name() string {
	return "expire"
}

func (g *expireCmd) Args() []string {
	return g.input
}
func (g *expireCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	g.result = string(bs)
	return nil
}

var expireHandler = CmdHandlers{
	name: "expire",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
