package kevago

import (
	"github.com/duongcongtoai/kevago/proto"
)

type infoCmd struct {
	result string
}

func (g *infoCmd) Name() string {
	return "info"
}

func (g *infoCmd) Args() []string {
	return nil
}
func (g *infoCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	g.result = string(bs)
	return nil
}

var infoHandler = CmdHandlers{
	name: "info",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
