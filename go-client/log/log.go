package log

import (
	"context"
	"fmt"
	"log"
	"os"
)

type Logging interface {
	Printf(ctx context.Context, format string, v ...interface{})
}

type logger struct {
	log *log.Logger
}

func (l *logger) Printf(ctx context.Context, format string, v ...interface{}) {
	_ = l.log.Output(2, fmt.Sprintf(format, v...))
}

// Logger calls Output to print to the stderr.
// Arguments are handled in the manner of fmt.Print.
var Logger Logging = &logger{
	log: log.New(os.Stderr, "keva: ", log.LstdFlags|log.Lshortfile),
}
