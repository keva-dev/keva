package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.nio.AcceptSocketHandler;
import com.jinyframework.keva.store.NoHeapConfig;
import com.jinyframework.keva.store.NoHeapFactory;
import com.jinyframework.keva.store.NoHeapStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Deprecated
public class NioServer implements IServer {

    private final ConfigHolder config;
    private AsynchronousServerSocketChannel server;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public NioServer(ConfigHolder config) {
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
        final AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
        server = AsynchronousServerSocketChannel.open(group);
        server.bind(new InetSocketAddress(config.getHostname(), config.getPort()));
    }

    @Override
    public void run() {
        initStorage();
        try {
            initServer();
        } catch (IOException e) {
            log.error("Failed to initialize server", e);
        }
        server.accept(null, new AcceptSocketHandler(server));
        try {
            System.in.read();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        try {
            server.close();
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
        executor.shutdown();
        boolean graceful = false;
        try {
            graceful = executor.awaitTermination(60, TimeUnit.SECONDS);
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
