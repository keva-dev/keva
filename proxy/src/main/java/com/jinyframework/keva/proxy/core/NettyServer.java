package com.jinyframework.keva.proxy.core;

import com.jinyframework.keva.proxy.ServiceInstance;
import com.jinyframework.keva.proxy.config.ConfigHolder;
import com.jinyframework.keva.server.core.IServer;
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

    public void bootstrapStorage() {
        val noHeapConfig = NoHeapConfig.builder()
                                       .heapSize(64)
                                       .snapshotEnabled(false)
                                       .snapshotLocation("./")
                                       .build();
        final NoHeapStore noHeapStore = NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
        ServiceInstance.getStorageService().setStore(noHeapStore);

        val storageName = noHeapStore.getName();
        log.info("Bootstrapped " + storageName);
    }

    public void bootstrapShards() throws Exception {
        val servers = config.getServerList().split(",");
        if (servers.length == 0 || servers[0].isEmpty()) {
            throw new Exception("No shard server defined");
        }
        for (String server : servers) {
            ServiceInstance.getLoadBalancingService().addShard(server, config.getVirtualNodeAmount());
            log.info("Added shard server: " + server);
        }
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            bootstrapStorage();
            bootstrapShards();
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
