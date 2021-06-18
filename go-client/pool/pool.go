package pool

import (
	"context"
	"fmt"
	"net"
	"sync/atomic"
	"time"

	"github.com/tuhuynh27/keva/go-client/log"
)

/*
TODO:
- expose metric
- add internal lock
- add test for error cases
*/
//ConnPool connection pool for keva
type ConnPool struct {
	opt               Options
	idleConns         []*Conn //TODO: consider between stack and queue based pool
	conns             map[string]*Conn
	totalManagedConns int //this counter is temporary and not accurate
	totalIdleConns    int
	sema              semaphore
	connsMu           *mutex
	closedChan        chan struct{}
	log               log.Logging

	runtime struct {
		totalErr      uint32
		lastDialError atomic.Value
	}
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
	Address            string
	PoolTimeout        time.Duration
	PoolSize           int
	MinIdleConn        int
	Dialer             func(ctx context.Context, addr string) (net.Conn, error)
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
		connsMu:    newMutex(),
		sema:       make(chan struct{}, opt.PoolSize),
		log:        log.Logger,
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

func (p *ConnPool) Close() {
	p.connsMu.Lock()
	for _, cn := range p.idleConns {
		p.closeConn(cn)
	}
	p.idleConns = nil
	for _, cn := range p.conns {
		delete(p.conns, cn.id)
	}
	p.totalManagedConns = 0
	p.totalIdleConns = 0
	p.connsMu.Unlock()
	close(p.closedChan)
	close(p.sema)
}

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

	//prevent spammy dialing
	netconn, err := p.dialIfErrorLazyRetry()
	if err != nil {
		return nil, err
	}

	p.connsMu.Lock()
	defer p.connsMu.Unlock()

	//Pool is full
	if p.totalManagedConns >= p.opt.PoolSize {
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

func (p *ConnPool) tryDialUntilSuccess() {
	for {
		if p.closed() {
			return
		}
		conn, err := p.opt.Dialer(context.Background(), p.opt.Address)
		if err != nil {
			p.runtime.lastDialError.Store(err)
			time.Sleep(time.Second)
			continue
		}
		atomic.StoreUint32(&p.runtime.totalErr, 0)
		conn.Close()
		return
	}
}

func (p *ConnPool) Put(c *Conn) {
	if p.closed() {
		p.closeConn(c)
		return
	}

	//TODO: check leftover read byte in socket
	if c.r.Buffered() > 0 {
		p.closeConn(c)
		p.sema.release()
		return
	}

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
	select {
	case <-p.closedChan:
		return true
	default:
		return false
	}
}

func (p *ConnPool) connReaper() {
	ticker := time.NewTicker(p.opt.IdleCheckFrequency)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			//in case time is clutch
			if p.closed() {
				return
			}
			p.reapStaleConns()
		case <-p.closedChan:
			return
		}
	}
}

func (p *ConnPool) getLastDialError() error {
	return p.runtime.lastDialError.Load().(error)
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

func (p *ConnPool) dialIfErrorLazyRetry() (net.Conn, error) {
	if atomic.LoadUint32(&p.runtime.totalErr) >= uint32(p.opt.PoolSize) {
		return nil, p.getLastDialError()
	}

	netconn, err := p.opt.Dialer(context.Background(), p.opt.Address)
	if err != nil {
		p.runtime.lastDialError.Store(err)
		//ensure only one goroutines execute this retry
		if atomic.AddUint32(&p.runtime.totalErr, 1) == uint32(p.opt.PoolSize) {
			go p.tryDialUntilSuccess()
		}
		return nil, err
	}
	return netconn, nil
}

func (p *ConnPool) addIdleConn() error {
	//prevent spammy dialing
	if atomic.LoadUint32(&p.runtime.totalErr) >= uint32(p.opt.PoolSize) {
		return p.getLastDialError()
	}
	netconn, err := p.dialIfErrorLazyRetry()
	if err != nil {
		return err
	}

	c := newManagedConnFromNet(netconn)
	p.connsMu.Lock()
	p.conns[c.id] = c
	p.idleConns = append(p.idleConns, c)
	//do not increment meta counter here
	p.connsMu.Unlock()
	return nil
}
