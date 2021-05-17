package pool

import (
	"context"
	"fmt"
	"net"
	"sync"
	"sync/atomic"
	"time"
)

var (
	maxErrConnect = 10
)

//ConnPool connectin pool to keva
//TODO: expose metrics
type ConnPool struct {
	opt               Options
	idleConns         []*Conn //TODO: consider between stack and queue based pool
	conns             map[string]*Conn
	totalManagedConns int //this counter is temporary and not accurate
	totalIdleConns    int
	sema              semaphore
	connsMu           *sync.Mutex
	lastDialError     atomic.Value
	closedChan        chan struct{}
}

//TotalConn to expose metric
func (p *ConnPool) TotalConns() int {
	p.connsMu.Lock()
	l := p.totalManagedConns
	p.connsMu.Unlock()
	return l
}

//TotalIdleConns to expose metric
func (p *ConnPool) TotalIdleConns() int {
	p.connsMu.Lock()
	l := len(p.idleConns)
	p.connsMu.Unlock()
	return l
}

//Options
type Options struct {
	PoolTimeout        time.Duration
	PoolSize           int
	MinIdleConn        int
	Dialer             func(ctx context.Context) (net.Conn, error)
	IdleTimeout        time.Duration
	MaxConnAge         time.Duration
	IdleCheckFrequency time.Duration
	OnConnClosed       func(*Conn) error
}

func setDefaultOpt(opt Options) Options {
	return opt
}

func NewConnPool(opt Options) (*ConnPool, error) {
	if opt.Dialer == nil {
		return nil, fmt.Errorf("missing dialing function")
	}
	opt = setDefaultOpt(opt)
	p := &ConnPool{
		opt:        opt,
		conns:      make(map[string]*Conn),
		idleConns:  make([]*Conn, 0, opt.PoolSize),
		closedChan: make(chan struct{}),
		connsMu:    new(sync.Mutex),
		sema:       make(chan struct{}, opt.PoolSize),
	}
	p.connsMu.Lock()
	p.ensureMinIdleConns()
	p.connsMu.Unlock()
	if opt.IdleCheckFrequency > 0 && opt.IdleTimeout > 0 {
		go p.connReaper()
	}

	return p, nil
}
func (p *ConnPool) popIdle() *Conn {
	if len(p.idleConns) == 0 {
		return nil
	}
	idx := len(p.idleConns) - 1
	p.totalIdleConns--
	cn := p.idleConns[idx]
	p.idleConns = p.idleConns[:idx]
	p.ensureMinIdleConns()
	return cn
}

//TODO
func (p *ConnPool) Close() {}

func (p *ConnPool) Get() (*Conn, error) {
	err := p.sema.acquireWithTimeout(p.opt.PoolTimeout)
	if err != nil {
		//someplaces are holding connections for so long
		return nil, err
	}
	for {
		p.connsMu.Lock()
		cn := p.popIdle() // stack based pool
		p.connsMu.Unlock()
		if cn == nil {
			break //no more idle, create new conn
		}
		if p.isStale(cn) {
			p.connsMu.Lock()
			//idle connection is always managed, remove here as well
			delete(p.conns, cn.id)
			p.totalManagedConns--
			p.connsMu.Unlock()
			continue
		}
		//metrics hit pool
		return cn, nil
	}

	netconn, err := p.opt.Dialer(context.Background())
	if err != nil {
		p.sema.release()
		return nil, err
	}
	p.connsMu.Lock()
	defer p.connsMu.Unlock()

	//Pool is full
	if p.totalManagedConns > p.opt.PoolSize {
		c := newUnmanagedConnFromNet(netconn) // will not be put back to pool later
		return c, nil
	} else {
		c := newManagedConnFromNet(netconn)
		p.totalManagedConns++
		p.totalIdleConns++
		p.conns[c.id] = c
		return c, nil
	}
}

func (p *ConnPool) Put(c *Conn) {
	//TODO: check leftover read byte in socket
	if !c.managed {
		p.closeConn(c)
		p.sema.release()
		return
	}
	p.connsMu.Lock()
	p.idleConns = append(p.idleConns, c)
	p.totalIdleConns++
	p.connsMu.Unlock()
	p.sema.release()
}

func (p *ConnPool) closed() bool {
	_, stillOpen := <-p.closedChan
	return !stillOpen
}

func (p *ConnPool) connReaper() {
	ticker := time.NewTicker(p.opt.IdleCheckFrequency)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			if p.closed() {
				return
			}
			p.reapStaleConns()
		case <-p.closedChan:
			return
		}
	}
}

//must used with lock
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

func (p *ConnPool) getLastDialError() error {
	return p.lastDialError.Load().(error)
}

//RemoveConn input connection should never be an idle connection
func (p *ConnPool) RemoveConn(c *Conn) {
	p.connsMu.Lock()

	//unmanaged
	delete(p.conns, c.id)
	p.totalManagedConns--

	//check min idle
	p.ensureMinIdleConns()
	p.connsMu.Unlock()
	p.sema.release()
	p.closeConn(c)
}

func (p *ConnPool) addIdleConn(totalErr *uint32) error {
	if atomic.LoadUint32(totalErr) >= uint32(maxErrConnect) {
		return p.getLastDialError()
	}
	netconn, err := p.opt.Dialer(context.Background())
	if err != nil {
		p.lastDialError.Store(err)
		atomic.AddUint32(totalErr, 1)
		return err
	}
	p.connsMu.Lock()
	c := newManagedConnFromNet(netconn)
	p.conns[c.id] = c
	p.idleConns = append(p.idleConns, c)
	//do not increment meta counter here
	p.connsMu.Unlock()
	return nil
}
