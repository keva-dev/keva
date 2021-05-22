package kevago

import (
	"github.com/duongcongtoai/kevago/proto"
)

type setCmd struct {
	input  []string
	result string
}

func (g *setCmd) Name() string {
	return "set"
}

func (g *setCmd) Args() []string {
	return g.input
}
func (g *setCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	g.result = string(bs)
	return nil
}

var setHandler = CmdHandlers{
	name: "set",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
