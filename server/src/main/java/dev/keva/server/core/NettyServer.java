package dev.keva.server.core;

import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.keva.server.command.setup.CommandService;
import dev.keva.server.command.setup.CommandServiceImpl;
import dev.keva.server.config.ConfigHolder;
import dev.keva.store.StorageService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyServer implements Server {
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final ConfigHolder config;

    // Executors
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private CommandService commandService;
    private Channel channel;
    private StorageService storageService;

    public NettyServer(ConfigHolder config) {
        this.config = config;
    }

    private void initServices() {
        Injector injector = Guice.createInjector(new CoreModule(config));
        commandService = new CommandServiceImpl(injector);
        storageService = injector.getInstance(StorageService.class);
    }

    public ServerBootstrap bootstrapServer() {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new RedisCodecInitializer(new ServerHandler(commandService)))
                .option(ChannelOption.SO_BACKLOG, 100)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_RCVBUF, BUFFER_SIZE)
                .childOption(ChannelOption.SO_SNDBUF, BUFFER_SIZE)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true);
        return b;
    }

    private void initExecutors() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        storageService.shutdownGracefully();

        channel.close();

        log.info("Keva server at {} stopped", config.getPort());
    }

    @Override
    public void run() {
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            initExecutors();
            initServices();
            ServerBootstrap server = bootstrapServer();
            final ChannelFuture sync = server.bind(config.getPort()).sync();
            log.info("Keva server started at {} in {} ms", config.getPort(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.stop();

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
}