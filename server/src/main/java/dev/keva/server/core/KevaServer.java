package dev.keva.server.core;

import com.google.common.base.Stopwatch;
import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.annotation.ComponentScan;
import dev.keva.server.command.mapping.CommandMapper;
import dev.keva.server.config.KevaConfig;
import dev.keva.store.KevaDatabase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ComponentScan("dev.keva.server")
public class KevaServer implements Server {
    private static final String KEVA_BANNER = "\n" +
            "  _  __  ___  __   __    _   \n" +
            " | |/ / | __| \\ \\ / /   /_\\  \n" +
            " | ' <  | _|   \\ V /   / _ \\ \n" +
            " |_|\\_\\ |___|   \\_/   /_/ \\_\\";

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channel;

    private final KevaDatabase database;
    private final KevaConfig config;
    private final NettyChannelInitializer nettyChannelInitializer;
    private final CommandMapper commandMapper;

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    @Autowired
    public KevaServer(KevaDatabase database, KevaConfig config, NettyChannelInitializer nettyChannelInitializer, CommandMapper commandMapper) {
        this.database = database;
        this.config = config;
        this.nettyChannelInitializer = nettyChannelInitializer;
        this.commandMapper = commandMapper;
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

    public ServerBootstrap bootstrapServer() {
        commandMapper.init();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        return new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(nettyChannelInitializer)
                .option(ChannelOption.SO_BACKLOG, 100)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true);
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
            val sync = server.bind(config.getPort()).sync();
            log.info("{} server started at {}:{}, PID: {}, in {} ms",
                    KEVA_BANNER,
                    config.getHostname(), config.getPort(),
                    ProcessHandle.current().pid(),
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
        database.clear();
    }
}
