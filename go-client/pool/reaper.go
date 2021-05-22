package pool

import (
	"time"
)

func (p *ConnPool) reapStaleConns() {
	for {
		p.sema.acquire()
		p.connsMu.Lock()
		cn := p.reapOneStaled()
		p.connsMu.Unlock()
		p.sema.release()
		if cn == nil {
			break
		}
		p.closeConn(cn)
	}
}

func (p *ConnPool) reapOneStaled() *Conn {
	if len(p.idleConns) == 0 {
		return nil
	}

	cn := p.idleConns[0]
	if !p.isStale(cn) {
		return nil
	}
	p.idleConns = append(p.idleConns[:0], p.idleConns[1:]...)
	p.totalIdleConns--
	//idle connection is always managed, remove here as well

	delete(p.conns, cn.id)
	p.totalManagedConns--
	p.ensureMinIdleConns()

	return cn

}

func (p *ConnPool) isStale(c *Conn) bool {
	if p.opt.IdleTimeout == 0 && p.opt.MaxConnAge == 0 {
		return false
	}
	now := time.Now()
	//long time not used
	if p.opt.IdleTimeout > 0 && now.Sub(c.LastUsed()) >= p.opt.IdleTimeout {
		return true
	}
	//recently used but total age reached
	if p.opt.MaxConnAge > 0 && now.Sub(c.createdAt) >= p.opt.MaxConnAge {
		return true
	}
	return false
}

func (p *ConnPool) closeConn(c *Conn) error {
	if p.opt.OnConnClosed != nil {
		p.opt.OnConnClosed(c)
	}

	return c.c.Close()
}
