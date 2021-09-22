package sentinel

import (
	"sort"
	"strings"
	"time"
)

func (s *slaveInstance) shutdown() {
	s.mu.Lock()
	s.killed = true
	s.mu.Unlock()
}

func (s *slaveInstance) iskilled() bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.killed
}

// This function is called in context current master is subj down only
// Redis version of this function also supports when master is alive, so some logic is missing
func (s *Sentinel) selectSlave(m *masterInstance) *slaveInstance {
	m.mu.Lock()
	defer m.mu.Unlock()
	// TODO
	maxDownTime := time.Since(m.downSince) + m.sentinelConf.DownAfter*10
	qualifiedSlaves := slaveCandidates{}
	for idx := range m.slaves {
		slave := m.slaves[idx]
		locked(&slave.mu, func() {
			if slave.sDown {
				return
			}
			if time.Since(slave.lastSucessfulPingAt) > 5*time.Second {
				return
			}
			if slave.slavePriority == 0 {
				return
			}
			infoValidityTime := 5 * time.Second
			if time.Since(slave.lastSucessfulInfoAt) > infoValidityTime {
				return
			}

			// accept the fact that this slave still does not see master is down somehow
			if slave.masterDownSinceSec > maxDownTime {
				return
			}

			// copy to compare, avoid locking
			qualifiedSlaves = append(qualifiedSlaves, slaveCandidate{
				slavePriority: slave.slavePriority,
				replOffset:    slave.replOffset,
				runID:         slave.runID,
				findBack:      slave,
			})
		})
	}
	if len(qualifiedSlaves) > 0 {
		sort.Sort(qualifiedSlaves)
		chosen := qualifiedSlaves[len(qualifiedSlaves)-1]
		return chosen.findBack
	}
	return nil
}

func (s *Sentinel) slaveInfoRoutine(sl *slaveInstance) {
	infoDelay := time.Duration(10 * time.Second)
	timer := time.NewTimer(infoDelay)
	for !sl.iskilled() {
		info, err := sl.client.Info()
		if err != nil {
			s.logger.Errorf("sl.client.Info: %s", err)
			//TODO continue for now
			continue
		} else {
			sl.mu.Lock()
			sl.lastSucessfulInfoAt = time.Now()
			sl.mu.Unlock()
		}
		roleSwitched, err := s.parseInfoSlave(sl.reportedMaster, sl.addr, info)
		if err != nil {
			s.logger.Errorf("parseInfoslave error: %v", err)
			continue
			// continue for now
		}

		// slave change master post failover logic
		// TODO

		if roleSwitched {
			panic("unimlemented")
			//TODO
			// s.parseInfoSlave()
		}
		timer.Reset(infoDelay)
		select {
		case <-sl.masterDownNotify:
			infoDelay = 1 * time.Second
		case <-timer.C:
		}
	}
}

func (s *Sentinel) slaveRoutine(sl *slaveInstance) {
	go s.slaveInfoRoutine(sl)
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for !sl.iskilled() {
		_, err := sl.client.Ping()
		sl.mu.Lock()
		if err != nil {
			if time.Since(sl.lastSucessfulInfoAt) > sl.reportedMaster.sentinelConf.DownAfter {
				sl.sDown = true
			}
		} else {
			sl.lastSucessfulPingAt = time.Now()
		}
		sl.mu.Unlock()
		<-ticker.C
	}
}

type slaveCandidates []slaveCandidate
type slaveCandidate struct {
	slavePriority int
	replOffset    int
	runID         string
	findBack      *slaveInstance
}

func (sl slaveCandidates) Len() int      { return len(sl) }
func (sl slaveCandidates) Swap(i, j int) { sl[i], sl[j] = sl[j], sl[i] }
func (sl slaveCandidates) Less(i, j int) bool {
	sli, slj := sl[i], sl[j]
	if sli.slavePriority != slj.slavePriority {
		return sli.slavePriority-slj.slavePriority < 0
	}
	if sli.replOffset > slj.replOffset {
		return false
	} else if sli.replOffset < slj.replOffset {
		return true
	}
	// equal replication offset, compare lexicongraphically
	cmp := strings.Compare(sli.runID, slj.runID)
	switch cmp {
	case -1:
		return true
	case 0:
		return true
	default:
		return false
	}
}
