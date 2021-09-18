package sentinel

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_parseInfoSlave(t *testing.T) {

}

// func Test_parseInfoMaster(t *testing.T) {
// 	s := &Sentinel{
// 		masterInstances: map[string]*masterInstance{},
// 		mu:              &sync.Mutex{},
// 	}
// 	s.masterInstances["fake_master"] = &masterInstance{
// 		slaves: map[string]*slaveInstance{},
// 	}
// 	info := `role:master
// run_id:5a4dfcf77726cfcda754cbdc0a42d1cec6a7348d
// connected_slaves:1
// slave0:ip=127.0.0.1,port=6379,state=online,offset=42,lag=0
// master_failover_state:no-failover
// 	`
// 	roleSwitched, err := s.parseInfoMaster("fake_master", info)
// 	assert.False(t, roleSwitched)
// 	assert.NoError(t, err)
// 	m := s.masterInstances["fake_master"]

// 	assert.Equal(t, "5a4dfcf77726cfcda754cbdc0a42d1cec6a7348d", m.runID)

// 	assert.Equal(t, 1, len(m.slaves))
// 	slave, exist := m.slaves["127.0.0.1:6379"]
// 	assert.True(t, exist)
// 	assert.Equal(t, "127.0.0.1:6379", slave.addr)
// 	assert.Equal(t, 42, slave.replOffset)
// }
func Test_slaveInfoRegexp(t *testing.T) {

	ok := slaveInfoRegexp.MatchString("ip=127.0.0.1,port=abc,state=online,offset=0,lag=0")

	assert.False(t, ok)
	ok = slaveInfoRegexp.MatchString("ip=127.0.0.1,port=123,state=something,offset=0,lag=0")
	assert.False(t, ok)
	ok = slaveInfoRegexp.MatchString("ip=127.0.0.1,port=123,state=offline,offset=abc,lag=0")
	assert.False(t, ok)
	ok = slaveInfoRegexp.MatchString("ip=127.0.0.1,port=123,state=online,offset=0,lag=abc")
	assert.False(t, ok)
	ok = slaveInfoRegexp.MatchString("ip=127.0.0.1,port=123,state=online,offset=0,lag=0")
	assert.True(t, ok)
	matches := slaveInfoRegexp.FindStringSubmatch("ip=127.0.0.1,port=123,state=online,offset=43,lag=74")
	assert.Equal(t, 6, len(matches))
	assert.Equal(t, "127.0.0.1", matches[1])
	assert.Equal(t, "123", matches[2])
	assert.Equal(t, "online", matches[3])
	assert.Equal(t, "43", matches[4])
	assert.Equal(t, "74", matches[5])
}
