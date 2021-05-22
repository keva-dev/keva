package kevago

import (
	"fmt"

	"github.com/duongcongtoai/kevago/proto"
)

type pingCmd struct {
	result error
}

func (g *pingCmd) Name() string {
	return "ping"
}

func (g *pingCmd) Args() []string {
	return nil
}
func (g *pingCmd) ReadResult(r *proto.Reader) error {
	bs, _, err := r.ReadLine()
	if err != nil {
		return err
	}
	if string(bs) != "PONG" {
		return fmt.Errorf("ping returned %s instead of PONG", string(bs))
	}
	return nil
}

var pingHandler = CmdHandlers{
	name: "ping",
	read: func(r *proto.Reader, c Cmd) error {
		return c.ReadResult(r)
	},
}
