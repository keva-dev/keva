package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.core.RedisCodecInitializer;
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
    private static final String SYNC_FORMAT = "PSYNC %s %s %s %s\n";
    private final String masterHost;
    private final int masterPort;
    private final LinkedBlockingDeque<CompletableFuture<Object>> resFutureQueue = new LinkedBlockingDeque<>();
    private final FutureHandler futureHandler = new FutureHandler(resFutureQueue);
    private Channel channel;

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
         .handler(new RedisCodecInitializer(futureHandler));
        final ChannelFuture future = b.connect(masterHost, masterPort).awaitUninterruptibly();
        if (future.isSuccess()) {
            channel = future.channel();
            return true;
        } else {
            return false;
        }
    }

    public CompletableFuture<Object> sendSync(String slaveHost, int slavePort) {
        return sendSync(slaveHost, slavePort, null, 0);
    }

    public CompletableFuture<Object> sendSync(String slaveHost, int slavePort,
                                              String masterId, int offset) {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        if (resFutureQueue.offer(future)) {
            final String syncMsg = String.format(SYNC_FORMAT, slaveHost, slavePort, masterId, offset);
            channel.writeAndFlush(syncMsg);
        } else {
            future.completeExceptionally(new IllegalStateException("Protocol client queue failure"));
        }
        return future;
    }
}
