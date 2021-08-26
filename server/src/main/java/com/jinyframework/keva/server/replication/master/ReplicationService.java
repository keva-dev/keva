package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

public interface ReplicationService {
    void initWriteLog(int size);

    String getReplicationId();

    Object performSync(String host, String port, String masterId, int offset) throws IOException;

    ConcurrentMap<String, Replica> getReplicas();

    void addReplica(String key);

    void filterAndBuffer(CommandName cmd, String line);
}
