package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.command.setup.CommandHandler;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.protocol.redis.BulkReply;
import com.jinyframework.keva.server.replication.master.Replica;
import com.jinyframework.keva.server.replication.master.ReplicationService;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class Info implements CommandHandler {
    private final ReplicationService replicationService;
    private final ConnectionService connectionService;

    public Info(ReplicationService replicationService, ConnectionService connectionService) {
        this.replicationService = replicationService;
        this.connectionService = connectionService;
    }

    @Override
    public BulkReply handle(List<String> args) {
        final HashMap<String, Object> stats = new HashMap<>();
        final long currentConnectedClients = connectionService.getCurrentConnectedClients();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients", currentConnectedClients);
        stats.put("threads", threads);
        final ConcurrentMap<String, Replica> replicas = replicationService.getReplicas();

        final ArrayList<Map.Entry<String, Replica>> entries = new ArrayList<>(replicas.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue().getJoinedTime()));
        final String slaveInfoFormat = "(host:%s, port:%d, online:%b)";
        int index = 0;
        for (Map.Entry<String, Replica> entry : entries) {
            final Replica replica = entry.getValue();
            final String slaveInfo = String.format(slaveInfoFormat,
                    replica.getHost(), replica.getPort(), replica.alive());
            stats.put("slave" + index, slaveInfo);
            index++;
        }
        stats.put("replicas", index);

        return new BulkReply(stats.toString());
    }
}
