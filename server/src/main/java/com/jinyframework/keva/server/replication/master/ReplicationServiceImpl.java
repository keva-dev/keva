package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ReplicationServiceImpl implements ReplicationService {
    private final ConcurrentHashMap<String, Replica> replicas = new ConcurrentHashMap<>();
    private final Set<CommandName> writeCommands = EnumSet.of(CommandName.SET, CommandName.DEL);

    private static InetSocketAddress parseSlave(String addr) {
        final String[] s = addr.split(":");
        final String host = s[0];
        final int port = Integer.parseInt(s[1]);
        return new InetSocketAddress(host, port);
    }

    @Override
    public ConcurrentMap<String, Replica> getReplicas() {
        return replicas;
    }

    @Override
    public void addReplica(String key) {
        if (replicas.containsKey(key)) {
            replicas.get(key).getLastCommunicated().getAndSet(System.currentTimeMillis());
            return;
        }
        final InetSocketAddress addr = parseSlave(key);
        final Replica rep = new Replica(addr.getHostName(), addr.getPort());
        rep.startWorker();
        replicas.put(key, rep);
    }

    @Override
    public void filterAndBuffer(CommandName cmd, String line) {
        if (!writeCommands.contains(cmd)) {
            return;
        }
        for (Map.Entry<String, Replica> entry : replicas.entrySet()) {
            try {
                entry.getValue().buffer(line);
            } catch (Exception e) {
                log.error("Failed to add command to replica buffer: ", e);
            }
        }
    }
}
