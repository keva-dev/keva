package dev.keva.core.server;

import com.google.common.base.Stopwatch;
import dev.keva.core.aof.AOFManager;
import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.store.KevaDatabase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.AbstractEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ComponentScan("dev.keva.core")
public class KevaServer implements Server {
    private static final String KEVA_BANNER = "\n" +
            "  _  __  ___  __   __    _   \n" +
            " | |/ / | __| \\ \\ / /   /_\\  \n" +
            " | ' <  | _|   \\ V /   / _ \\ \n" +
            " |_|\\_\\ |___|   \\_/   /_/ \\_\\";
    private static final int SHUTDOWN_TIMEOUT_MS = 1000;
    private enum State {
        CREATED, CREATING, RUNNING, TERMINATING, TERMINATED
    }

    private volatile State state;
    private final KevaDatabase database;
    private final KevaConfig config;
    private final NettyChannelInitializer nettyChannelInitializer;
    private final CommandMapper commandMapper;
    private final AOFManager aofManager;
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    @Autowired
    public KevaServer(KevaDatabase database, KevaConfig config, NettyChannelInitializer nettyChannelInitializer, CommandMapper commandMapper, AOFManager aofManager) {
        this.database = database;
        this.config = config;
        this.nettyChannelInitializer = nettyChannelInitializer;
        this.commandMapper = commandMapper;
        this.aofManager = aofManager;
        this.state = State.CREATED;
    }

    public static KevaServer ofDefaults() {
        KevaIoC context = KevaIoC.initBeans(KevaServer.class);
        return context.getBean(KevaServer.class);
    }

    public static KevaServer of(KevaConfig config) {
        KevaIoC context = KevaIoC.initBeans(KevaServer.class, config);
        return context.getBean(KevaServer.class);
    }

    public static KevaServer ofCustomBeans(Object... beans) {
        KevaIoC context = KevaIoC.initBeans(KevaServer.class, beans);
        return context.getBean(KevaServer.class);
    }

    @Override
    public void shutdown() {
        switch (state) {
            case CREATED:
            case CREATING:
                throw new RuntimeException("Attempt to shutdown a non-started server!");
            case RUNNING:
                boolean set = updateState(State.TERMINATING);
                if (!set) {
                    // The state was concurrently modified, so re-check the condition.
                    shutdown();
                    return;
                }
                try {
                    bossGroup.shutdownGracefully(0, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS).sync();
                    workerGroup.shutdownGracefully(0, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS).sync();
                    channel.close();
                    database.close();
                    aofManager.stop();
                } catch (Exception e) {
                    log.warn("Encountered error while shutting down server, ignoring", e);
                }
                state = State.TERMINATED;
                log.info("Keva server at {} stopped", config.getPort());
                log.info("Bye bye!");
                return;
            default:
        }
    }

    @Override
    public void run() {
        switch (state) {
            case CREATING:
            case RUNNING:
                return;
            case CREATED:
                // take create lock and call run again
                boolean set = updateState(State.CREATING);
                if (!set) {
                    // The state was concurrently modified, so re-check the condition.
                    run();
                    return;
                }
                try {
                    stopwatch.start();
                    ServerBootstrap server = bootstrapServer();

                    aofManager.init();

                    ChannelFuture sync = server.bind(config.getPort()).sync();
                    log.info("{} server started at {}:{}, in {} ms",
                            KEVA_BANNER,
                            config.getHostname(), config.getPort(),
                            stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    log.info("Ready to accept connections");
                    state = State.RUNNING;
                    System.out.println("Set state to running");
                    channel = sync.channel();
                    channel.closeFuture().sync(); //block
                } catch (InterruptedException e) {
                    log.error("Failed to start server: ", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Failed to start server: ", e);
                } finally {
                    stopwatch.stop();
                }
                return;
            default:
                throw new RuntimeException("Attempt to run a stopped server");
        }
    }

    @Override
    public void clear() {
        switch (state) {
            case RUNNING:
                database.flush();
                return;
            default:
                throw new RuntimeException("Attempt to clear a non-running server");
        }

    }

    // Do a CAS on state
    private synchronized boolean updateState(State state) {
        if (this.state.equals(state)) {
            return false;
        }
        this.state = state;
        return true;
    }

    private ServerBootstrap bootstrapServer() throws NettyNativeTransportLoader.NettyNativeLoaderException {
        try {
            commandMapper.init();
            Class<? extends AbstractEventExecutorGroup> executorGroupClazz = NettyNativeTransportLoader.getEventExecutorGroupClazz();
            bossGroup = (EventLoopGroup) executorGroupClazz.getDeclaredConstructor(int.class).newInstance(1);
            workerGroup = (EventLoopGroup) executorGroupClazz.getDeclaredConstructor().newInstance();
            return new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NettyNativeTransportLoader.getServerSocketChannelClazz())
                    .childHandler(nettyChannelInitializer)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.TCP_NODELAY, true);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
            throw new NettyNativeTransportLoader.NettyNativeLoaderException("Cannot load Netty classes");
        }
    }
}
