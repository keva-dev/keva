package kevago

import (
	"github.com/duongcongtoai/kevago/proto"
)

type delCmd struct {
	input  []string
	result string
}

func (g *delCmd) Name() string {
	return "del"
}

func (g *delCmd) Args() []string {
	return g.input
}
func (g *delCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	g.result = string(bs)
	return nil
}

var delHandler = CmdHandlers{
	name: "del",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
