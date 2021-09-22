package sentinel

import (
	"bufio"
	"fmt"
	"io"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"
)

func (s *Sentinel) parseInfoSlave(m *masterInstance, slaveAddr, info string) (bool, error) {
	r := bufio.NewScanner(strings.NewReader(info))
	r.Split(bufio.ScanLines)
	role, err := s.preparseInfo(r)
	if err != nil {
		return false, err
	}
	if role != instanceRoleSlave {
		return true, nil
	}

	m.mu.Lock()

	slaveIns, ok := m.slaves[slaveAddr]
	if !ok {
		slaveIns = &slaveInstance{mu: sync.Mutex{}}
		m.slaves[slaveAddr] = slaveIns //LOGIC of new slave here
	}
	m.mu.Unlock()

	//to compare later
	masterAddr := fmt.Sprintf("%s:%s", m.ip, m.port)

	slaveIns.mu.Lock()
	defer slaveIns.mu.Unlock()
	//use slave's lock for now
	for r.Scan() {
		if r.Err() != nil {
			panic("not reached")
		}
		line := r.Text()
		if strings.HasPrefix(line, "run_id:") {
			if len(slaveIns.runID) == 0 {
				slaveIns.runID = line[7:]
			} else {
				if len(slaveIns.runID) != len(line[7:]) && slaveIns.runID != line[7:] {
					//reboot logic
					slaveIns.runID = line[7:]
				}
			}
			continue
		}
		//master_link_down_since_seconds:-1
		if len(line) >= 32 && line[:30] == "master_link_down_since_seconds" {
			intSec, err := strconv.Atoi(line[:30])
			if err != nil {
				continue
			}
			slaveIns.masterDownSinceSec = time.Duration(intSec)
			continue
		}
		if len(line) >= 12 && line[:12] == "master_host:" {
			slaveIns.masterHost = line[12:]
			continue
		}
		if len(line) >= 12 && line[:12] == "master_port:" {
			slaveIns.masterPort = line[12:]
			continue
		}

		if len(line) >= 19 && line[:19] == "master_link_status:" {
			switch line[19:] {
			case "up":
				slaveIns.masterUp = true
			case "down":
				slaveIns.masterUp = false
			default:
				//invalid line
				continue
			}
			continue
		}
		if len(line) >= 15 && line[:15] == "slave_priority:" {
			priority, err := strconv.Atoi(line[15:])
			if err != nil {
				continue
			}
			slaveIns.slavePriority = priority
			continue
		}
		if len(line) >= 18 && line[:18] == "slave_repl_offset:" {
			offset, err := strconv.Atoi(line[18:])
			if err != nil {
				continue
			}
			slaveIns.replOffset = offset
			continue
		}

	}
	currentMasterAddr := fmt.Sprintf("%s:%s", slaveIns.masterHost, slaveIns.masterPort)
	if currentMasterAddr != masterAddr {
		s.logger.Errorf("master of slave mismatch, has %s, but slave reported to serve %s, sentinel does not support slave switching master",
			currentMasterAddr, masterAddr,
		)
		//HANDLE switch master
		//send slaveof to this slave to make this slave serve correct master again
	}
	return false, nil
	//re acquire master lock, check if slave is still there and then spawn goroutine for this slave

}

// syntax of info replied by keva is different from redis. We need to know at the beginning
// of the string the role of the instance first
func (s *Sentinel) parseInfoMaster(masterAddress string, info string) (bool, error) {
	r := bufio.NewScanner(strings.NewReader(info))
	r.Split(bufio.ScanLines)
	role, err := s.preparseInfo(r)
	if err != nil {
		return false, err
	}
	if role != instanceRoleMaster {
		return true, nil
	}
	s.mu.Lock()
	m, ok := s.masterInstances[masterAddress]
	s.mu.Unlock()
	if !ok {
		err := fmt.Errorf("master %s does not exist", masterAddress)
		s.logger.Errorf(err.Error())
		return false, err
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	slaveCheck := map[string]bool{}
	for key := range m.slaves {
		slaveCheck[key] = false
	}
	newSlaves := []*slaveInstance{}

	for r.Scan() {
		if r.Err() != nil {
			panic("not reached")
		}
		line := r.Text()
		if strings.HasPrefix(line, "run_id:") {
			if len(m.runID) == 0 {
				m.runID = line[7:]
			} else {
				if len(m.runID) != len(line[7:]) && m.runID != line[7:] {
					//reboot logic
					m.runID = line[7:]
				}
			}
			continue
		}
		// slave0:ip=127.0.0.1,port=6379,state=online,offset=42,lag=0
		if strings.HasPrefix(line, "slave") {
			parts := strings.Split(line[5:], ":")
			if len(parts) == 2 {
				_, err := strconv.Atoi(parts[0])
				if err != nil {
					fmt.Printf("invalid line: %s", line)
					continue
				}

				matches := slaveInfoRegexp.FindStringSubmatch(parts[1])
				if len(matches) != 6 {
					s.logger.Errorf("invalid slave info syntax: %s", parts[1])
					continue
				}
				replOffset, _ := strconv.Atoi(matches[4])
				addr := fmt.Sprintf("%s:%s", matches[1], matches[2])
				_, exist := m.slaves[addr]

				if !exist {
					newslave := &slaveInstance{
						masterHost:       m.ip,
						masterPort:       m.port,
						addr:             addr,
						replOffset:       replOffset,
						reportedMaster:   m,
						masterDownNotify: make(chan struct{}, 1),
					}
					err := s.slaveFactory(newslave)
					if err != nil {
						s.logger.Errorf("s.slaveFactory: %s", err)
						continue
					}
					newSlaves = append(newSlaves, newslave)

				}
				//TODO: check if indx of slave matters
				//temporarily use addr to define uniqueness for now
			} //check if slave exist, if not create new and spawn new ping routine for slave
			//invalid syntax, skip this line
			continue
		}

	}
	for key, keep := range slaveCheck {
		if !keep {
			m.slaves[key].shutdown()
			delete(m.slaves, key)
			// despawn goroutine of this slave and remove it
		}
	}
	for _, item := range newSlaves {
		m.slaves[item.addr] = item
		go s.slaveRoutine(item)
		//spawn goroutine for new slave
	}
	return false, nil
}

var (
	slaveInfoRegexp = regexp.MustCompile("ip=(.*),port=([0-9]+),state=(online|offline),offset=([0-9]+),lag=([0-9]+)")
)

// syntax of info replied by keva is different from redis. We need to know at the beginning
// of the string the role of the instance first
func (s *Sentinel) preparseInfo(r *bufio.Scanner) (role instanceRole, err error) {
	found := r.Scan()
	if !found {
		if r.Err() == nil {
			err = io.EOF
			return
		}
		err = r.Err()
	}
	line := r.Text()

	if !strings.HasPrefix(line, "role:") {
		err = fmt.Errorf("invalid info reply, first line must contain role")
		return
	}
	switch line[5:] {
	case "master":
		role = instanceRoleMaster
	case "slave":
		role = instanceRoleSlave
	default:
		err = fmt.Errorf("invalid role: %s", line[5:])
	}
	return

}
