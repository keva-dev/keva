package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.command.CommandRegistrar;
import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.command.CommandServiceImpl;
import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.replication.master.ReplicationServiceImpl;
import com.jinyframework.keva.server.replication.slave.SlaveService;
import com.jinyframework.keva.server.replication.slave.SlaveServiceImpl;
import com.jinyframework.keva.server.storage.NoHeapStorageServiceImpl;
import com.jinyframework.keva.server.storage.StorageService;
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
    private final ConfigHolder config;
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // Should only use 1 thread to handle to keep order of commands for now
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    private ConnectionService connectionService;
    private StorageService storageService;
    private SlaveService slaveService;
    private CommandService commandService;
    private ReplicationService replicationService;

    public NettyServer(ConfigHolder config) {
        this.config = config;
        initServices();
    }

    private void initServices() {
        connectionService = new ConnectionServiceImpl();
        storageService = new NoHeapStorageServiceImpl();
        slaveService = new SlaveServiceImpl();
        replicationService = new ReplicationServiceImpl();

        final CommandRegistrar commandRegistrar = new CommandRegistrar(storageService, replicationService, connectionService);
        commandService = new CommandServiceImpl(commandRegistrar.getHandlerMap(), replicationService);
    }

    public ServerBootstrap bootstrapServer() {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.TRACE))
         .childHandler(new StringCodecLineFrameInitializer(new ServerHandler(connectionService, commandService)));
        return b;
    }

    public void bootstrapReplication() throws IOException, ExecutionException, InterruptedException {
        if (config.getReplicaOf() != null && !config.getReplicaOf().isBlank() && !"NO:ONE".equalsIgnoreCase(config.getReplicaOf())) {
            // start slave service and sync snapshot file in blocking manner
            slaveService.start(config);
        }
        replicationService.initWriteLog(config.getWriteLogSize());
    }

    public void bootstrapStorage() {
        val noHeapConfig = NoHeapConfig.builder()
                                       .heapSize(config.getHeapSize())
                                       .snapshotEnabled(config.getSnapshotEnabled())
                                       .snapshotLocation(config.getSnapshotLocation())
                                       .build();
        final NoHeapStore noHeapStore = NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
        storageService.setStore(noHeapStore);
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
            log.info("Database server started at {}", config.getPort());

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
