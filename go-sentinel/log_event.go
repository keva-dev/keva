package sentinel

var (
	logEventBecameTermLeader     = "sentinel_became_leader"
	logEventNeighborVotedFor     = "neighbor_sentinel_voted_for"
	logEventVotedFor             = "sentinel_voted_for"
	logEventFailoverStateChanged = "failover_state_change"
	logEventSelectedSlave        = "slave_selected"
)
