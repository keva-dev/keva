package proto

import "bufio"

type Reader struct {
	*bufio.Reader
}

type Writer struct {
	*bufio.Writer
}
