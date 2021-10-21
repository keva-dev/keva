package dev.keva.server.replication.master;

import dev.keva.server.command.setup.CommandName;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

public interface ReplicationService {
    String getReplicationId();

    Object performSync(String host, String port, String masterId, int offset) throws IOException;

    ConcurrentMap<String, Replica> getReplicas();

    void addReplica(String key);

    void filterAndBuffer(CommandName cmd, String line);
}
