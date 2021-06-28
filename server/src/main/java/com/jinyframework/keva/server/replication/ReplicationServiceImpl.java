package com.jinyframework.keva.server.replication;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReplicationServiceImpl implements ReplicationService {
    private final ConcurrentHashMap<String, ReplicaInfo> replicas = new ConcurrentHashMap<>();

    @Override
    public ConcurrentMap<String, ReplicaInfo> getReplicas() {
        return replicas;
    }

    @Override
    public void addReplica(SocketAddress address) {
        final ReplicaInfo info = ReplicaInfo.builder()
                                            .lastCommunicated(new AtomicLong(System.currentTimeMillis()))
                                            .build();
        replicas.put(String.valueOf(address), info);
    }
}
