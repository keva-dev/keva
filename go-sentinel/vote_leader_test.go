package sentinel

import (
	"sync"
	"testing"

	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
)

func Test_VoteLeader(t *testing.T) {
	s := &Sentinel{
		mu:     &sync.Mutex{},
		logger: zap.NewNop().Sugar(),
	}
	m := &masterInstance{
		mu: sync.Mutex{},
	}
	leaderEpoch, leaderID := s.voteLeader(m, 1, "123")
	assert.Equal(t, leaderID, "123")
	assert.Equal(t, leaderEpoch, 1)

	leaderEpoch, leaderID = s.voteLeader(m, 0, "1234")
	assert.Equal(t, leaderID, "123")
	assert.Equal(t, leaderEpoch, 1)

	leaderEpoch, leaderID = s.voteLeader(m, 1, "1234")
	assert.Equal(t, leaderID, "123")
	assert.Equal(t, leaderEpoch, 1)

	leaderEpoch, leaderID = s.voteLeader(m, 2, "1234")
	assert.Equal(t, leaderID, "1234")
	assert.Equal(t, leaderEpoch, 2)
}
