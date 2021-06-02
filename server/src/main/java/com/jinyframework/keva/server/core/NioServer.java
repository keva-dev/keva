package com.jinyframework.keva.server.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jinyframework.keva.engine.ChronicleStringKevaMap;
import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.nio.AcceptSocketHandler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class NioServer implements IServer {

    private final ConfigHolder config;
    private AsynchronousServerSocketChannel server;
    private ExecutorService executor;

    public NioServer(ConfigHolder config) {
        this.config = config;
    }

    private void initStorage() {
        val filePath = config.getSnapshotLocation();
        try {
            val chronicleStringKevaMap = !config.getSnapshotEnabled() ?
                                         new ChronicleStringKevaMap() :
                                         new ChronicleStringKevaMap(filePath);
            ServiceInstance.getStorageService().setEngine(chronicleStringKevaMap);
            log.info("Bootstrapped engine");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private void initServer() throws IOException {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
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
        if (server.isOpen()) {
            try {
                server.close();
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
        log.info("Database server stopped");
    }
}
