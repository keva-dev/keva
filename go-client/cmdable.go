package kevago

import (
	"fmt"
	"time"
)

type commandAdaptor func(Cmd) error

//TODO: remove boiler plating code, reuse code according to input/output type
func (c commandAdaptor) Get(key string) (string, error) {
	comd := &getCmd{
		input: []string{key},
	}
	err := c(comd)
	return comd.result, err
}

func (c commandAdaptor) Set(key string, value string) (string, error) {
	comd := &setCmd{
		input: []string{key, value},
	}
	err := c(comd)
	return comd.result, err
}

func (c commandAdaptor) Delete(key string) (string, error) {
	comd := &delCmd{
		input: []string{key},
	}
	err := c(comd)
	return comd.result, err
}

func (c commandAdaptor) Ping() error {
	comd := &pingCmd{}
	return c(comd)
}

func (c commandAdaptor) Info() (string, error) {
	comd := &infoCmd{}
	err := c(comd)

	return comd.result, err
}
func (c commandAdaptor) Expire(key string, d time.Duration) (string, error) {
	comd := &expireCmd{
		input: []string{fmt.Sprintf("%s %d", key, d.Milliseconds())},
	}
	err := c(comd)
	return comd.result, err
}
