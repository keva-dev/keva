package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.master.Replica;

import java.lang.management.ManagementFactory;
import java.util.*;
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

        return stats.toString();
    }
}
