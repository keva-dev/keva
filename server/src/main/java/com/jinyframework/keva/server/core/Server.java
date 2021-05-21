package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.store.NoHeapConfig;
import com.jinyframework.keva.store.NoHeapFactory;
import com.jinyframework.keva.store.NoHeapStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Server implements IServer {
    private static final long HEARTBEAT_TIMEOUT = 60000;
    private static final int SHUTDOWN_TIMEOUT = 5;

    private final AtomicBoolean serverStopping = new AtomicBoolean(false);
    private final AtomicBoolean serverStopped = new AtomicBoolean(false);

    private final ConfigHolder config;
    private java.net.ServerSocket serverSocket;
    private ExecutorService executor;

    public Server(ConfigHolder config) {
        this.config = config;
    }

    private void initStorage() {
        val noHeapConfig = NoHeapConfig.builder()
                .heapSize(config.getHeapSize())
                .snapshotEnabled(config.getSnapshotEnabled())
                .snapshotLocation(config.getSnapshotLocation())
                .build();
        final NoHeapStore noHeapStore = NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
        ServiceInstance.getStorageService().setStore(noHeapStore);

        val storageName = noHeapStore.getName();
        log.info("Bootstrapped " + storageName);
    }

    private void initServer() throws IOException {
        val host = config.getHostname();
        val port = config.getPort();
        executor = Executors.newCachedThreadPool();
        val socketAddress = new InetSocketAddress(host, port);
        if (serverSocket == null) {
            serverSocket = new java.net.ServerSocket();
        }
        serverSocket.bind(socketAddress);

        log.info("Database server started on {}:{}", host, port);
    }

    private void initHeartbeat() {
        if (config == null || config.getHeartbeatEnabled() == null || !config.getHeartbeatEnabled()) {
            return;
        }
        Long heartbeatTimeout = config.getHeartbeatTimeout();
        if (heartbeatTimeout <= 0) {
            heartbeatTimeout = HEARTBEAT_TIMEOUT;
        }
        val heartbeatInterval = heartbeatTimeout / 2;

        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(ServiceInstance.getConnectionService()
                        .getHeartbeatRunnable(heartbeatTimeout),
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        log.info("Heartbeat service started");
    }

    @Override
    public void run() {
        initStorage();
        initHeartbeat();
        try {
            initServer();
        } catch (IOException e) {
            log.error("Failed to initialize server", e);
        }
        while (!Thread.interrupted() && !serverStopping.get()) {
            try {
                val socket = serverSocket.accept();
                if (serverStopping.get()) {
                    socket.close();
                    break;
                }
                executor.execute(() -> {
                    val kevaSocket = ServerSocket.builder()
                            .socket(socket)
                            .id(UUID.randomUUID().toString())
                            .lastOnlineLong(new AtomicLong(System.currentTimeMillis()))
                            .alive(new AtomicBoolean(true))
                            .build();
                    ServiceInstance.getConnectionService().handleConnection(kevaSocket);
                });
            } catch (SocketException | SocketTimeoutException ignore) {
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        serverStopped.set(true);
    }

    @Override
    public void shutdown() {
        serverStopping.set(true);
        if (serverSocket != null && !serverSocket.isClosed() && serverStopped.get()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        executor.shutdown();
        boolean graceful = false;
        try {
            graceful = executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        if (!graceful) {
            log.error("Graceful shutdown timed out");
        }
        log.info("Database server stopped");
    }
}
