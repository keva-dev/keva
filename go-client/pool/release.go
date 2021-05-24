// +build !debug

package pool

import (
	"context"
	"sync"
)

func (p *ConnPool) ensureMinIdleConns() {
	if p.opt.MinIdleConn < 0 {
		return
	}

	for p.totalManagedConns < p.opt.PoolSize && p.totalIdleConns < p.opt.MinIdleConn {
		p.totalManagedConns++ //TODO: is this counter necessary
		p.totalIdleConns++
		go func() {
			err := p.addIdleConn()

			//min idle may not be guaranteed
			if err != nil {
				p.log.Printf(context.Background(), "failed to establish new connection: %s", err)
				p.connsMu.Lock()
				p.totalManagedConns--
				p.totalIdleConns--
				p.connsMu.Unlock()
			}
		}()
	}
}

type mutex struct {
	sync.Mutex
}

func newMutex() *mutex {
	return &mutex{
		sync.Mutex{},
	}

}
