package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.CommandName;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
public class ReplicationServiceImpl implements ReplicationService {
    private final ConcurrentHashMap<String, Replica> replicas = new ConcurrentHashMap<>();
    private final Set<CommandName> writeCommands = EnumSet.of(CommandName.SET, CommandName.DEL);
    private final ScheduledExecutorService healthCheckerPool = Executors.newScheduledThreadPool(1);
    private final ExecutorService repWorkerPool = Executors.newCachedThreadPool();

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
        final InetSocketAddress addr = parseSlave(key);
        if (replicas.containsKey(key)) {
            replicas.get(key).getLastCommunicated().getAndSet(System.currentTimeMillis());
            return;
        }
        final Replica rep = new Replica(addr.getHostName(), addr.getPort());
        replicas.put(key, rep);
        repWorkerPool.submit(() -> {
            rep.connect();
            if (!rep.alive()) {
                log.warn("Couldn't connect to slave");
                return;
            }
            final CompletableFuture<Object> lostFuture = new CompletableFuture<>();
            final ScheduledFuture<?> scheduleFuture = rep.startHealthChecker(healthCheckerPool, lostFuture);
            lostFuture.whenComplete((res, ex) -> {
                log.warn("Slave connection lost");
                scheduleFuture.cancel(true);
                replicas.remove(key);
            });
            rep.commandRelayTask().run();
        });
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
