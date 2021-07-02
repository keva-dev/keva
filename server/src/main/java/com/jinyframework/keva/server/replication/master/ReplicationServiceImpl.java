package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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
        final ReplicaClient replicaClient = new ReplicaClient(addr.getHostName(), addr.getPort());
        final Replica rep = Replica.builder()
                                   .lastCommunicated(new AtomicLong(System.currentTimeMillis()))
                                   .cmdBuffer(new LinkedBlockingQueue<>())
                                   .client(replicaClient)
                                   .build();
        new Thread(() -> {
            while (true) {
                try {
                    final String line = rep.getCmdBuffer().take();
                    final Promise<Object> send = rep.getClient().send(line);
                    if (send.isSuccess()) {
                        final long now = System.currentTimeMillis();
                        rep.getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
                    }
                } catch (Exception e) {
                    log.error("Failed to forward command: ", e);
                }
            }
        }, "rep-" + key + "-thread").start();
        replicas.put(key, rep);
    }

    @Override
    public void filterAndBuffer(CommandName cmd, String line) {
        if (!writeCommands.contains(cmd)) {
            return;
        }
        for (Map.Entry<String, Replica> entry : replicas.entrySet()) {
            try {
                entry.getValue().cmdBuffer.add(line);
            } catch (Exception e) {
                log.error("Failed to add command to replica buffer: ", e);
            }
        }
    }
}
