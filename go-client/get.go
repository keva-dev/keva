package kevago

import (
	"github.com/tuhuynh27/keva/go-client/proto"
)

type getCmd struct {
	input  []string
	result string
}

func (g *getCmd) Name() string {
	return "get"
}

func (g *getCmd) Args() []string {
	return g.input
}
func (g *getCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	g.result = string(bs)
	return nil
}

var getHandler = CmdHandlers{
	name: "get",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
