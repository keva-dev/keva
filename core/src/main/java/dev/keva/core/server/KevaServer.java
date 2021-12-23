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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import static dev.keva.util.Constants.NUM_WORKERS;

@Slf4j
@Component
@ComponentScan("dev.keva.core")
public class KevaServer implements Server {
    private static final String KEVA_BANNER = "\n" +
            "  _  __  ___  __   __    _   \n" +
            " | |/ / | __| \\ \\ / /   /_\\  \n" +
            " | ' <  | _|   \\ V /   / _ \\ \n" +
            " |_|\\_\\ |___|   \\_/   /_/ \\_\\";
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
        val context = KevaIoC.initBeans(KevaServer.class, beans);
        return context.getBean(KevaServer.class);
    }

    public ServerBootstrap bootstrapServer() throws NettyNativeTransportLoader.NettyNativeLoaderException {
        try {
            commandMapper.init();
            val executorGroupClazz = NettyNativeTransportLoader.getEventExecutorGroupClazz();
            bossGroup = (EventLoopGroup) executorGroupClazz.getDeclaredConstructor(int.class).newInstance(1);
            workerGroup = (EventLoopGroup) executorGroupClazz.getDeclaredConstructor(int.class).newInstance(NUM_WORKERS);
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

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channel.close();
        log.info("Keva server at {} stopped", config.getPort());
        log.info("Bye bye!");
    }

    @Override
    public void run() {
        try {
            stopwatch.start();
            val server = bootstrapServer();

            aofManager.init();

            val sync = server.bind(config.getPort()).sync();
            log.info("{} server started at {}:{}, in {} ms",
                    KEVA_BANNER,
                    config.getHostname(), config.getPort(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
            log.info("Ready to accept connections");

            channel = sync.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Failed to start server: ", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to start server: ", e);
        } finally {
            stopwatch.stop();
        }
    }

    @Override
    public void clear() {
        database.flush();
    }
}
