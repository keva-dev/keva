package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReplicationServiceImpl implements ReplicationService {
    private final ConcurrentHashMap<String, ReplicaInfo> replicas = new ConcurrentHashMap<>();

    private final Set<CommandName> writeCommands = EnumSet.of(CommandName.SET, CommandName.DEL);

    @Override
    public ConcurrentMap<String, ReplicaInfo> getReplicas() {
        return replicas;
    }

    @Override
    public void addReplica(String key) {
        if (replicas.containsKey(key)) {
            replicas.get(key).getLastCommunicated().getAndSet(System.currentTimeMillis());
            return;
        }
        final ReplicaInfo info = ReplicaInfo.builder()
                                            .lastCommunicated(new AtomicLong(System.currentTimeMillis()))
                                            .cmdBuffer(new ConcurrentLinkedQueue<>())
                                            .build();
        replicas.put(key, info);
    }

    @Override
    public void filterAndBuffer(CommandName cmd, String line) {
        if (!writeCommands.contains(cmd)) {
            return;
        }
        for (Map.Entry<String, ReplicaInfo> entry : replicas.entrySet()) {
            entry.getValue().cmdBuffer.add(line);
        }
    }
}
