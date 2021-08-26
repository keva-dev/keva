package sentinel

import "time"

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

func (s *Sentinel) slaveInfoRoutine(sl *slaveInstance) {
	infoTicker := time.NewTicker(10 * time.Second)
	for !sl.iskilled() {
		select {
		case <-infoTicker.C:
			info, err := sl.client.Info()
			if err != nil {
				logger.Errorf("sl.client.Info: %s", err)
				//TODO continue for now
				continue
			}
			roleSwitched, err := s.parseInfoSlave(sl.masterName, sl.addr, info)
			if err != nil {
				logger.Errorf("parseInfoMaster: %v", err)
				continue
				// continue for now
			}
			if roleSwitched {
				panic("unimlemented")
				//TODO
				// s.parseInfoSlave()
			}
		case <-sl.masterDownNotify:
			infoTicker.Reset(time.Second)
		}
	}
}

func (s *Sentinel) slaveRoutine(sl *slaveInstance) {
	go s.slaveInfoRoutine(sl)
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	for !sl.iskilled() {
		<-ticker.C
		_, err := sl.client.Ping()
		if err != nil {
			logger.Errorf("Ping: %s", err)
		}

	}
}
