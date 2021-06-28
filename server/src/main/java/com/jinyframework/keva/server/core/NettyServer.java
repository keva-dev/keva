package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.store.NoHeapConfig;
import com.jinyframework.keva.store.NoHeapFactory;
import com.jinyframework.keva.store.NoHeapStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class NettyServer implements IServer{
    private final ConfigHolder config;

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyServer(ConfigHolder config) {
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

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            initStorage();
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ServerChannelInitializer());

            b.bind(config.getPort()).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Failed to start server: ",e);
            Thread.currentThread().interrupt();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
