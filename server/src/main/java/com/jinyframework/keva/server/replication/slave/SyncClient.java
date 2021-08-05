package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.core.StringCodecLineFrameInitializer;
import com.jinyframework.keva.server.replication.FutureHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A TCP client used to make request to master
 */
public class SyncClient {
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final String masterHost;
    private final int masterPort;
    private Channel channel;
    private final LinkedBlockingDeque<CompletableFuture<Object>> resFutureQueue = new LinkedBlockingDeque<>();
    private final FutureHandler futureHandler = new FutureHandler(resFutureQueue);

    public SyncClient(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public boolean connect() {
        final Bootstrap b = new Bootstrap();
        b.group(workerGroup)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .handler(new LoggingHandler(LogLevel.INFO))
         .handler(new StringCodecLineFrameInitializer(futureHandler));
        final ChannelFuture future = b.connect(masterHost, masterPort).awaitUninterruptibly();
        if (future.isSuccess()) {
            channel = future.channel();
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Object> fullSync(String slaveHost, Integer slavePort) {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        if (resFutureQueue.offer(future)) {
            channel.writeAndFlush("FSYNC " + slaveHost + ' ' + slavePort + '\n');
        } else {
            future.completeExceptionally(new Exception("Protocol client failure"));
        };
        return future;
    }
}
