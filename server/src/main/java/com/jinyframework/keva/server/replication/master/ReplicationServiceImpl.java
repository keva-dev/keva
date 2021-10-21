package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.command.setup.CommandName;
import com.jinyframework.keva.server.core.WriteLog;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class ReplicationServiceImpl implements ReplicationService {
    private static final String SYNC_RESP_FORMAT = "%s %s %d %s";
    private final Set<CommandName> writeCommands = EnumSet.of(CommandName.SET, CommandName.DEL);
    private final ScheduledExecutorService healthCheckerPool;
    private final ExecutorService repWorkerPool;
    private final ConcurrentHashMap<String, Replica> replicas = new ConcurrentHashMap<>();
    private final StorageService storageService;
    private final String replicationId;
    private final WriteLog writeLog;

    public ReplicationServiceImpl(ScheduledExecutorService healthCheckerPool, ExecutorService repWorkerPool, StorageService storageService, WriteLog writeLog) {
        this.healthCheckerPool = healthCheckerPool;
        this.repWorkerPool = repWorkerPool;
        this.storageService = storageService;
        this.writeLog = writeLog;
        replicationId = UUID.randomUUID().toString();
    }

    private static InetSocketAddress parseSlave(String addr) {
        final String[] s = addr.split(":");
        final String host = s[0];
        final int port = Integer.parseInt(s[1]);
        return new InetSocketAddress(host, port);
    }

    @Override
    public String getReplicationId() {
        return replicationId;
    }

    @Override
    public Object performSync(String host, String port, String masterId, int offset) throws IOException {
        final String response;
        final Base64.Encoder encoder = Base64.getEncoder();
        addReplica(host + ':' + port);
        if (masterId == null || masterId.isBlank()
                || offset < writeLog.getMinOffset() || !replicationId.equals(masterId)) {
            // perform a full sync
            // F {masterId} {currentOffset} {syncFileContent}
            final String content = encoder.encodeToString(
                    Files.readAllBytes(Path.of(storageService.getSnapshotPath() + "/" + "dump.kdb")));
            response = String.format(SYNC_RESP_FORMAT, 'F', replicationId, writeLog.getCurrentOffset(), content);
        } else {
            // perform a partial sync
            // P {masterId} {currentOffset} {encodedListOfCommands}
            ArrayList<String> list = writeLog.copyFromOffset(offset);
            log.info("psync commands: {}", list);
            final String commands = encoder.encodeToString(String.join("\n",
                    list).getBytes(StandardCharsets.UTF_8));
            response = String.format(SYNC_RESP_FORMAT, 'P', masterId, writeLog.getCurrentOffset(), commands);
        }
        return response;
    }

    @Override
    public ConcurrentMap<String, Replica> getReplicas() {
        return replicas;
    }

    @Override
    public void addReplica(String key) {
        final InetSocketAddress addr = parseSlave(key);
        if (replicas.containsKey(key)) {
            // should check to reconnect here
            return;
        }
        final Replica rep = new Replica(addr.getHostName(), addr.getPort());
        replicas.put(key, rep);
        repWorkerPool.submit(() -> {
            rep.connect();
            if (!rep.alive()) {
                log.warn("Couldn't connect to slave: {}:{}", rep.getHost(), rep.getPort());
                return;
            }
            final CompletableFuture<Object> lostConn = new CompletableFuture<>();
            final ScheduledFuture<?> healthChecking = healthCheckerPool.scheduleAtFixedRate(rep.healthChecker(lostConn),
                    5, 1, TimeUnit.SECONDS);
            lostConn.whenComplete((res, ex) -> {
                log.warn("Slave connection lost");
                healthChecking.cancel(true);
            });
            rep.commandRelayTask().run();
        });
    }

    @Override
    public void filterAndBuffer(CommandName cmd, String line) {
        if (!writeCommands.contains(cmd)) {
            return;
        }
        writeLog.buffer(line);
        for (Map.Entry<String, Replica> entry : replicas.entrySet()) {
            final Replica rep = entry.getValue();
            if (rep.alive()) {
                rep.buffer(line);
            }
        }
    }
}
