package dev.keva.server.core;

import com.google.common.base.Stopwatch;
import dev.keva.server.config.ConfigHolder;
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
public class NettyServer implements Server {
    private final ConfigHolder config = AppFactory.getConfig();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channel;

    public NettyServer() {
    }

    public ServerBootstrap bootstrapServer() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        return new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NettyChannelInitializer(new NettyChannelHandler()))
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
    }

    @Override
    public void run() {
        try {
            val stopwatch = Stopwatch.createStarted();
            val server = bootstrapServer();
            val sync = server.bind(config.getPort()).sync();
            log.info("Keva server initialized at {}:{}, PID: {}, in {} ms",
                    config.getHostname(), config.getPort(),
                    ProcessHandle.current().pid(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
            log.info("Ready to accept connections");
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
