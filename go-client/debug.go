// +build debug

package kevago

import (
	"sync"
)

//must used with lock
func (p *ConnPool) ensureMinIdleConns() {
	if p.connsMu.getIsLocked() == false {
		panic("ensureMinIdleConns must be called within a mutex lock")
	}
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
	isLocked   bool
	statusLock sync.Mutex
}

func newMutex() *mutex {
	return &mutex{
		Mutex:      sync.Mutex{},
		statusLock: sync.Mutex{},
	}
}

func (p *mutex) Lock() {
	p.Mutex.Lock()
	p.toggleStatusLocked()
}

func (p *mutex) toggleStatusLocked() {
	p.statusLock.Lock()
	p.isLocked = !p.isLocked
	p.statusLock.Unlock()
}

func (p *mutex) getIsLocked() bool {
	p.statusLock.Lock()
	defer p.statusLock.Unlock()
	return p.isLocked
}

func (p *mutex) Unlock() {
	p.toggleStatusLocked()
	p.Mutex.Unlock()
}
