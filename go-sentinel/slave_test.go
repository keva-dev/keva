package sentinel

import (
	"fmt"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func newQualifiedSlave(id string) *slaveInstance {
	return &slaveInstance{
		runID:               id,
		mu:                  sync.Mutex{},
		slavePriority:       1,
		lastSucessfulInfoAt: time.Now(),
		lastSucessfulPingAt: time.Now(),
	}
}

func Test_selectSlave(t *testing.T) {
	t.Run("test unqualified slaves", func(t *testing.T) {
		slaves := []*slaveInstance{}
		s := &Sentinel{mu: &sync.Mutex{}}
		m := &masterInstance{mu: sync.Mutex{}}
		//sdown
		slaves = append(slaves, &slaveInstance{
			runID: fmt.Sprintf("slave-%d", 1),
			mu:    sync.Mutex{},
			sDown: true,
		})

		// lost ping for too long
		slaves = append(slaves, &slaveInstance{
			runID:               fmt.Sprintf("slave-%d", 2),
			mu:                  sync.Mutex{},
			lastSucessfulPingAt: time.Now().Add(-time.Hour),
		})

		// lost info for too long
		slaves = append(slaves, &slaveInstance{
			runID:               fmt.Sprintf("slave-%d", 3),
			mu:                  sync.Mutex{},
			lastSucessfulInfoAt: time.Now().Add(-time.Hour),
		})

		//qualified
		slaves = append(slaves, newQualifiedSlave(fmt.Sprintf("slave-%d", 4)))
		m.slaves = map[string]*slaveInstance{}
		for _, item := range slaves {
			m.slaves[item.runID] = item
		}
		slaveInstance := s.selectSlave(m)
		assert.NotNil(t, slaveInstance)
		assert.Equal(t, slaveInstance.runID, fmt.Sprintf("slave-%d", 4))
	})
	t.Run("test slave ordering by priority", func(t *testing.T) {
		s := &Sentinel{mu: &sync.Mutex{}}
		m := &masterInstance{mu: sync.Mutex{}}

		slave1 := newQualifiedSlave("1")
		slave2 := newQualifiedSlave("2")
		slave1.slavePriority = 1
		slave2.slavePriority = 2
		slaves := []*slaveInstance{slave1, slave2}
		m.slaves = map[string]*slaveInstance{}
		for _, item := range slaves {
			m.slaves[item.runID] = item
		}
		slaveInstance := s.selectSlave(m)
		assert.NotNil(t, slaveInstance)
		assert.Equal(t, "2", slaveInstance.runID)
	})
	t.Run("test slave ordering by repl offset", func(t *testing.T) {
		s := &Sentinel{mu: &sync.Mutex{}}
		m := &masterInstance{mu: sync.Mutex{}}

		slave1 := newQualifiedSlave("1")
		slave2 := newQualifiedSlave("2")
		slave1.replOffset = 1
		slave2.replOffset = 2

		slaves := []*slaveInstance{slave1, slave2}
		m.slaves = map[string]*slaveInstance{}
		for _, item := range slaves {
			m.slaves[item.runID] = item
		}
		slaveInstance := s.selectSlave(m)
		assert.NotNil(t, slaveInstance)
		assert.Equal(t, "2", slaveInstance.runID)
	})
	t.Run("test slave ordering by lexicongraphical", func(t *testing.T) {
		s := &Sentinel{mu: &sync.Mutex{}}
		m := &masterInstance{mu: sync.Mutex{}}

		slave1 := newQualifiedSlave("1")
		slave2 := newQualifiedSlave("2")
		slave1.replOffset = 1
		slave2.replOffset = 2

		slaves := []*slaveInstance{slave1, slave2}
		m.slaves = map[string]*slaveInstance{}
		for _, item := range slaves {
			m.slaves[item.runID] = item
		}
		slaveInstance := s.selectSlave(m)
		assert.NotNil(t, slaveInstance)
		assert.Equal(t, "2", slaveInstance.runID)
	})

}
