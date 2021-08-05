package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.master.Replica;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.jinyframework.keva.server.ServiceInstance.getConnectionService;
import static com.jinyframework.keva.server.ServiceInstance.getReplicationService;

public class Info implements CommandHandler {
    @Override
    public String handle(List<String> args) {
        final HashMap<String, Object> stats = new HashMap<>();
        final long currentConnectedClients = getConnectionService().getCurrentConnectedClients();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients", currentConnectedClients);
        stats.put("threads", threads);
        final ConcurrentMap<String, Replica> replicas = getReplicationService().getReplicas();

        int count = 0;
        for (Map.Entry<String, Replica> entry : replicas.entrySet()) {
            stats.put("slave" + count, entry.getValue());
            count++;
        }
        stats.put("replicas", count);

        return stats.toString();
    }
}
