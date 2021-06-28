package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.ReplicaInfo;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.jinyframework.keva.server.ServiceInstance.getConnectionService;
import static com.jinyframework.keva.server.ServiceInstance.getReplicationService;

public class Info implements CommandHandler {
    @Override
    public String handle(CommandContext ctx, List<String> args) {
        final HashMap<String, Object> stats = new HashMap<>();
        final long currentConnectedClients = getConnectionService().getCurrentConnectedClients();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients:", currentConnectedClients);
        stats.put("threads:", threads);
        final ConcurrentMap<String, ReplicaInfo> replicas = getReplicationService().getReplicas();

        stats.put("replicas:", replicas.size());
        int count = 0;
        for (Map.Entry<String, ReplicaInfo> entry : replicas.entrySet()) {
            stats.put("slave" + count + ':', entry.getValue());
            count++;
        }

        return stats.toString();
    }
}
