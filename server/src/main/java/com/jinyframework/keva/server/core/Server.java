package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceFactory;
import com.jinyframework.keva.server.config.ConfigHolder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Server {
    private static final long HEARTBEAT_TIMEOUT = 60000;
    private static final int SHUTDOWN_TIMEOUT = 5;

    private final AtomicBoolean serverStopping = new AtomicBoolean(false);
    private final AtomicBoolean serverStopped = new AtomicBoolean(false);

    private final ConfigHolder config;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    public Server(ConfigHolder config) {
        this.config = config;
    }

    private void startServer() throws IOException {
        val host = config.getHostname();
        val port = config.getPort();
        executor = Executors.newCachedThreadPool();
        val socketAddress = new InetSocketAddress(host, port);
        if (serverSocket == null) {
            serverSocket = new ServerSocket();
        }
        serverSocket.bind(socketAddress);

        log.info("Database server started on {}:{}", host, port);
    }

    private void startHeartbeat() {
        if (config == null || config.getHeartbeatEnabled() == null || !config.getHeartbeatEnabled()) {
            return;
        }

        Long heartbeatTimeout = config.getHeartbeatTimeout();
        if (heartbeatTimeout <= 0) {
            heartbeatTimeout = HEARTBEAT_TIMEOUT;
        }
        val heartbeatInterval = heartbeatTimeout / 2;

        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(ServiceFactory.getConnectionService().getHeartbeatRunnable(heartbeatTimeout),
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        log.info("Heartbeat service started");
    }

    private void startSnapshot() {
        if (config == null || config.getSnapshotEnabled() == null || !config.getSnapshotEnabled()) {
            return;
        }

        val recoveryPath = config.getRecoveryPath();
        if (recoveryPath != null && !recoveryPath.isEmpty()) {
            ServiceFactory.getSnapshotService().recover(recoveryPath);
        }

        val snapIntervalDur = Duration.parse(config.getSnapshotInterval());
        if (snapIntervalDur.toMillis() > 0) {
            ServiceFactory.getSnapshotService().start(snapIntervalDur, config.getBackupPath());
        }
    }

    public void run() throws IOException {
        startServer();
        startHeartbeat();
        startSnapshot();
        while (!serverStopping.get()) {
            try {
                val socket = serverSocket.accept();
                if (serverStopping.get()) {
                    socket.close();
                    break;
                }
                executor.execute(() -> {
                    val kevaSocket = KevaSocket.builder()
                            .socket(socket)
                            .id(UUID.randomUUID().toString())
                            .lastOnlineLong(new AtomicLong(System.currentTimeMillis()))
                            .alive(new AtomicBoolean(true))
                            .build();
                    ServiceFactory.getConnectionService().handleConnection(kevaSocket);
                });
            } catch (SocketException | SocketTimeoutException ignore) {
            }
        }
        serverStopped.set(true);
    }

    public void shutdown() throws Exception {
        serverStopping.set(true);
        if (serverSocket != null && !serverSocket.isClosed() && serverStopped.get()) {
            serverSocket.close();
        }
        executor.shutdown();
        val graceful = executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        if (!graceful) {
            log.error("Graceful shutdown timed out");
        }
        log.info("Database server stopped");
    }
}
