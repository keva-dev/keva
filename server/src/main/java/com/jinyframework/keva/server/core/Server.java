package com.jinyframework.keva.server.core;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
    private final String host;
    private final int port;
    private final long heartbeatTimeout;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private void init() throws IOException {
        executor = Executors.newCachedThreadPool();
        val socketAddress = new InetSocketAddress(host, port);
        if (serverSocket == null) {
            serverSocket = new ServerSocket();
        }
        serverSocket.bind(socketAddress);
        log.info("Database server started");
    }

    private void startHeartbeat() {
        val heartbeatInterval = heartbeatTimeout / 2;

        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(connectionService().getHeartbeatRunnable(heartbeatTimeout),
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        log.info("Heartbeat service started");
    }

    public void run() throws IOException {
        init();
        startHeartbeat();
        while (!Thread.interrupted()) {
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
        }
    }

    public void shutdown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        val graceful = executor.awaitTermination(5, TimeUnit.SECONDS);
        if (!graceful) {
            log.error("Graceful shutdown timed out");
        }
        log.info("Database server stopped");
    }
}
