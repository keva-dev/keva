package pool

import (
	"bufio"
	"net"
	"sync/atomic"
	"time"

	"github.com/duongcongtoai/kevago/proto"
	"github.com/google/uuid"
)

func (c *Conn) LastUsed() time.Time {
	unix := atomic.LoadInt64(&c.usedAt)
	return time.Unix(unix, 0)
}

//IsManaged is managed by the pool or not
func (c *Conn) IsManaged() bool {
	return c.managed
}

//Conn wrap around net Conn
type Conn struct {
	id        string
	c         net.Conn
	r         *proto.Reader
	w         *proto.Writer
	managed   bool
	createdAt time.Time
	usedAt    int64
}

//SetLastUsed let user modify connection status
func (c *Conn) SetLastUsed(t time.Time) {
	atomic.StoreInt64(&c.usedAt, t.Unix())
}

func newManagedConnFromNet(c net.Conn) *Conn {
	now := time.Now()
	return &Conn{
		w: &proto.Writer{
			Writer: bufio.NewWriter(c),
		},
		r: &proto.Reader{
			Reader: bufio.NewReader(c),
		},
		id:        uuid.New().String(),
		c:         c,
		managed:   true,
		createdAt: now,
		usedAt:    now.Unix(),
	}
}

func newUnmanagedConnFromNet(c net.Conn) *Conn {
	now := time.Now()
	return &Conn{
		w: &proto.Writer{
			Writer: bufio.NewWriter(c),
		},
		r: &proto.Reader{
			Reader: bufio.NewReader(c),
		},
		id:        uuid.New().String(),
		c:         c,
		managed:   false,
		createdAt: now,
		usedAt:    now.Unix(),
	}
}
