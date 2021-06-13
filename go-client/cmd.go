package kevago

import (
	"fmt"
	"strings"

	"github.com/tuhuynh27/keva/go-client/pool"
	"github.com/tuhuynh27/keva/go-client/proto"
)

type commandMap struct {
	internal map[string]CmdHandlers
}
type CmdHandlers struct {
	name string
	read func(r *proto.Reader, c Cmd) error
	//currently don't need write func
}

type Cmd interface {
	Name() string
	Args() []string
	ReadResult(r *proto.Reader) error
}

var globalCmds = commandMap{
	internal: make(map[string]CmdHandlers),
}

func init() {
	handlers := []CmdHandlers{
		getHandler, setHandler, delHandler, infoHandler, pingHandler, expireHandler,
	}
	for _, h := range handlers {
		registerCmd(h.name, h)
	}
}

func registerCmd(name string, h CmdHandlers) {
	globalCmds.internal[name] = h
}

func (c commandMap) execute(conn *pool.Conn, comd Cmd) error {
	hs, exist := c.internal[comd.Name()]
	if !exist {
		return fmt.Errorf("command %s not found", comd.Name())
	}
	err := conn.WriteIntercept(func(w *proto.Writer) error {
		_, err := w.WriteString(fmt.Sprintf("%s %s\n", comd.Name(), strings.Join(comd.Args(), " ")))
		if err != nil {
			return err
		}
		return w.Flush()

	})

	if err != nil {
		return err
	}
	return conn.ReadIntercept(func(w *proto.Reader) error {
		return hs.read(w, comd)
	})
}