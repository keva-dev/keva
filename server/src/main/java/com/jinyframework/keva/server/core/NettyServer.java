package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.store.NoHeapConfig;
import com.jinyframework.keva.store.NoHeapFactory;
import com.jinyframework.keva.store.NoHeapStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyServer implements IServer {
    static final ServerHandler SERVER_HANDLER = new ServerHandler();
    private final ConfigHolder config;
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // Should only use 1 thread to handle to keep order of commands
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);


    public NettyServer(ConfigHolder config) {
        this.config = config;
    }

    public ServerBootstrap bootstrapServer() {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new StringCodecLineFrameInitializer(SERVER_HANDLER));
        return b;
    }

    public void bootstrapReplication() throws IOException, ExecutionException, InterruptedException {
        if (config.getReplicaOf() != null && !config.getReplicaOf().isBlank() && !"NO:ONE".equalsIgnoreCase(config.getReplicaOf())) {
            // start slave service and sync snapshot file in blocking manner
            ServiceInstance.getSlaveService().start(config);
        } else {
            ServiceInstance.getReplicationService().init();
        }
    }

    public void bootstrapStorage() {
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

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            bootstrapReplication();
            bootstrapStorage();
            final ServerBootstrap server = bootstrapServer();
            final ChannelFuture sync = server.bind(config.getPort()).sync();

            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Failed to start server: ", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to start server: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
