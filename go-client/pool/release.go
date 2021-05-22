// +build !debug

package pool

import "sync"

func (p *ConnPool) ensureMinIdleConns() {
	if p.opt.MinIdleConn < 0 {
		return
	}
	totalErrCount := uint32(0)

	for p.totalManagedConns < p.opt.PoolSize && p.totalIdleConns < p.opt.MinIdleConn {
		p.totalManagedConns++ //TODO: is this counter necessary
		p.totalIdleConns++
		go func() {
			err := p.addIdleConn(&totalErrCount)

			//min idle may not be guaranteed
			if err != nil {
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
