package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.command.setup.CommandRegistrar;
import com.jinyframework.keva.server.command.setup.CommandService;
import com.jinyframework.keva.server.command.setup.CommandServiceImpl;
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class NettyServer implements IServer {
    private final ConfigHolder config;

    // Executors
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ExecutorService repWorkerPool;
    private ScheduledExecutorService healthCheckerPool;

    // Services
    private ConnectionService connectionService;
    private StorageService storageService;
    private SlaveService slaveService;
    private CommandService commandService;
    @Getter // use for testing should change to dedicated command or extract from INFO
    private ReplicationService replicationService;
    private WriteLog writeLog;
    private Channel channel;
    private ServerBootstrap server;
    private NoHeapStore noHeapStore;

    public NettyServer(ConfigHolder config) {
        this.config = config;
    }

    private void initServices(boolean isFreshStart) {
        if (isFreshStart) {
            writeLog = new WriteLog(config.getWriteLogSize());
        }
        initStorageService(isFreshStart);
        replicationService = new ReplicationServiceImpl(healthCheckerPool, repWorkerPool, storageService, writeLog);

        connectionService = new ConnectionServiceImpl();
        final CommandRegistrar commandRegistrar = new CommandRegistrar(storageService, replicationService, connectionService);
        commandService = new CommandServiceImpl(commandRegistrar.getHandlerMap(), replicationService);
        slaveService = new SlaveServiceImpl(healthCheckerPool, writeLog, commandService);
    }

    public ServerBootstrap bootstrapServer() {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(new RedisCodecInitializer(new ServerHandler(connectionService, commandService)));
        return b;
    }

    public void startSlaveService() throws Exception {
        if (config.getReplicaOf() != null && !config.getReplicaOf()
                .isBlank() && !"NO:ONE".equalsIgnoreCase(config.getReplicaOf())) {
            // start slave service and sync snapshot file in blocking manner
            slaveService.start(config);
        }
    }

    public void initStorageService(boolean isFreshStart) {
        val noHeapConfig = NoHeapConfig.builder()
                .heapSize(config.getHeapSize())
                .snapshotEnabled(config.getSnapshotEnabled())
                .snapshotLocation(config.getSnapshotLocation())
                .build();
        if (isFreshStart) {
            noHeapStore = NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
        }
        storageService = new NoHeapStorageServiceImpl(noHeapStore);
    }

    private void initExecutors() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        repWorkerPool = Executors.newCachedThreadPool();
        healthCheckerPool = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void shutdown() {
        repWorkerPool.shutdown();
        healthCheckerPool.shutdown();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channel.close();
        log.info("Database server at {} stopped", config.getPort());
    }

    @Override
    public void run(boolean isFreshStart) {
        try {
            initExecutors();
            initServices(isFreshStart);
            startSlaveService();
            server = bootstrapServer();
            final ChannelFuture sync = server.bind(config.getPort()).sync();
            log.info("Database server started at {}", config.getPort());

            channel = sync.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Failed to start server: ", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to start server: ", e);
        } finally {
            shutdown();
        }
    }

    @Override
    public void run() {
        run(true);
    }
}
