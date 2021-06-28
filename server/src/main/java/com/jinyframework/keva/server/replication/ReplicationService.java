package com.jinyframework.keva.server.replication;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

public interface ReplicationService {
    ConcurrentMap<String, ReplicaInfo> getReplicas();

    void addReplica(SocketAddress address);
}
