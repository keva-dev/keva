package kevago

import (
	"bufio"
	"fmt"
	"strings"
)

type internalCmdInfo struct {
	name string
}

type internalCmd interface {
	Name() string
	Args() []string
	ReadResult(r *bufio.Reader) error
}

var internalcmds = internalCmdMap{}

type internalCmdMap map[string]internalCmdInfo

func init() {
	cmds := []internalCmdInfo{
		{
			name: "info",
		},
	}
	for _, item := range cmds {
		internalcmds[item.name] = item
	}
}

func (c internalCmdMap) execute(conn *internalConn, comd internalCmd) error {
	_, exist := c[comd.Name()]
	if !exist {
		return fmt.Errorf("command %s not found", comd.Name())
	}
	_, err := conn.w.WriteString(fmt.Sprintf("%s %s\n", comd.Name(), strings.Join(comd.Args(), " ")))
	if err != nil {
		return err
	}
	err = conn.w.Flush()
	if err != nil {
		return err
	}

	if err != nil {
		return err
	}
	return comd.ReadResult(conn.r)
}
