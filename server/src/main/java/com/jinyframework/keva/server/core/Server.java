package com.jinyframework.keva.server.core;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.jinyframework.keva.server.ServiceFactory.connectionService;

@Slf4j
@Builder
public class Server {
    private static final long HEARTBEAT_TIMEOUT = 60000;
    private static final int SHUTDOWN_TIMEOUT = 5;

    private final AtomicBoolean serverStopping = new AtomicBoolean(false);
    private final AtomicBoolean serverStopped = new AtomicBoolean(false);

    private final String host;
    private final int port;
    private long heartbeatTimeout;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private void init() throws IOException {
        executor = Executors.newCachedThreadPool();
        val socketAddress = new InetSocketAddress(host, port);
        if (serverSocket == null) {
            serverSocket = new ServerSocket();
        }
        serverSocket.bind(socketAddress);
        serverSocket.setSoTimeout(SHUTDOWN_TIMEOUT);

        log.info("Database server started on {}:{}", host, port);
    }

    private void startHeartbeat() {
        if (heartbeatTimeout <= 0) {
            heartbeatTimeout = HEARTBEAT_TIMEOUT;
        }
        val heartbeatInterval = heartbeatTimeout / 2;

        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(connectionService().getHeartbeatRunnable(heartbeatTimeout),
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        log.info("Heartbeat service started");
    }

    public void run() throws IOException {
        init();
        startHeartbeat();
        while (!serverStopping.get()) {
            try {
                val socket = serverSocket.accept();
                executor.execute(() -> {
                    val kevaSocket = KevaSocket.builder()
                            .socket(socket)
                            .id(UUID.randomUUID().toString())
                            .lastOnlineLong(new AtomicLong(System.currentTimeMillis()))
                            .alive(new AtomicBoolean(true))
                            .build();
                    connectionService().handleConnection(kevaSocket);
                });
            } catch (SocketTimeoutException ignore) {
                continue;
            }
        }
        serverStopped.set(true);
    }

    public void shutdown() throws Exception {
        serverStopping.set(true);
        executor.shutdown();
        val graceful = executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        if (!graceful) {
            log.error("Graceful shutdown timed out");
        }
        if (serverSocket != null && !serverSocket.isClosed() && serverStopped.get()) {
            serverSocket.close();
        }
        log.info("Database server stopped");
    }
}
