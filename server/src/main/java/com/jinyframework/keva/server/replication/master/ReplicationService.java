package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;

import java.util.concurrent.ConcurrentMap;

public interface ReplicationService {
    ConcurrentMap<String, Replica> getReplicas();

    void addReplica(String key);

    void filterAndBuffer(CommandName cmd, String line);

    void init();
}
