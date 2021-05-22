package pool

import (
	"fmt"
	"time"
)

type semaphore chan struct{}

func (s semaphore) acquire() {
	s <- struct{}{}
}
func (s semaphore) release() {
	<-s
}

var (
	ErrPoolTimeout = fmt.Errorf("acquiring from pool timeout")
)

func (s semaphore) acquireWithTimeout(t time.Duration) error {
	timer := time.NewTimer(t)
	defer timer.Stop()
	select {
	case <-timer.C:
		return ErrPoolTimeout
	case s <- struct{}{}:
		return nil
	}
}
