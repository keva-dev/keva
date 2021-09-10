package sentinel

import (
	"go.uber.org/zap/zaptest/observer"
)

func (suite *testSuite) handleLogEventSentinelVotedFor(instanceIdx int, log observer.LoggedEntry) {
	ctxMap := log.ContextMap()

	votedFor := ctxMap["voted_for"].(string)
	term := int(ctxMap["epoch"].(int64))
	currentInstanceID := suite.mapIdxtoRunID[instanceIdx]

	suite.mu.Lock()
	defer suite.mu.Unlock()
	_, exist := suite.termsVote[term]
	if !exist {
		suite.termsVote[term] = make([]termInfo, len(suite.instances))
	}
	termInfo := suite.termsVote[term][instanceIdx]
	if termInfo.selfVote != "" {
		if termInfo.selfVote != votedFor {
			suite.t.Fatalf("instance %s voted for multiple instances (%s and %s) in the same term %d",
				currentInstanceID, termInfo.selfVote, votedFor, term)
		}
	}
	termInfo.selfVote = votedFor
	if termInfo.neighborVotes == nil {
		termInfo.neighborVotes = map[string]string{}
	}
	suite.termsVote[term][instanceIdx] = termInfo
}

func (suite *testSuite) handleLogEventBecameTermLeader(instanceIdx int, log observer.LoggedEntry) {
	ctxMap := log.ContextMap()
	leader := ctxMap["run_id"].(string)
	term := int(ctxMap["epoch"].(int64))
	suite.mu.Lock()
	defer suite.mu.Unlock()
	previousLeader, exist := suite.termsLeader[term]
	if exist {
		suite.t.Fatalf("term %d has multiple leader recognition event, previously is %s and current is %s",
			term, previousLeader, leader)
	}
	suite.termsLeader[term] = leader
	suite.currentLeader = leader
	suite.currentTerm = term
}

func (suite *testSuite) handleLogEventFailoverStateChanged(instanceIdx int, log observer.LoggedEntry) {
	ctxMap := log.ContextMap()
	newState := ctxMap["new_state"].(failOverState)

	suite.mu.Lock()
	defer suite.mu.Unlock()
	oldState := suite.failOverState
	switch newState {
	case failOverWaitLeaderElection:
		fallthrough
	case failOverSelectSlave:
		fallthrough
	case failOverPromoteSlave:
		fallthrough
	case failOverReconfSlave:
		if newState-oldState != 1 {
			suite.t.Fatalf("invalid failover state transition from %s to %s", oldState, newState)
		}
	case failOverNone:
		if oldState != failOverWaitLeaderElection {
			suite.t.Fatalf("can only transition from %s to %s, but has %s to %s",
				failOverWaitLeaderElection, failOverNone, oldState, failOverNone)
		}
	default:
		suite.t.Fatalf("invalid failover state: %d", newState)
	}
	suite.failOverState = newState
}

func (suite *testSuite) handleLogEventNeighborVotedFor(instanceIdx int, log observer.LoggedEntry) {
	ctxMap := log.ContextMap()
	term := int(ctxMap["epoch"].(int64))
	neighborID := ctxMap["neighbor_id"].(string)
	votedFor := ctxMap["voted_for"].(string)

	suite.mu.Lock()
	defer suite.mu.Unlock()
	_, exist := suite.termsVote[term]
	if !exist {
		suite.termsVote[term] = make([]termInfo, len(suite.instances))
	}
	termInfo := suite.termsVote[term][instanceIdx]

	if termInfo.neighborVotes == nil {
		termInfo.neighborVotes = map[string]string{}
	}
	previousRecordedVote := termInfo.neighborVotes[neighborID]

	// already record this neighbor vote before, check if it is consistent
	if previousRecordedVote != "" {
		if previousRecordedVote != votedFor {
			suite.t.Fatalf("neighbor %s is recorded to voted for different leaders (%s and %s) in the same term %d",
				neighborID, previousRecordedVote, votedFor, term,
			)
		}
	}
	termInfo.neighborVotes[neighborID] = votedFor
	suite.termsVote[term][instanceIdx] = termInfo
}
