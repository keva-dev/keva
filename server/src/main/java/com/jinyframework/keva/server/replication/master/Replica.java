package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.core.StringCodecLineFrameInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A TCP client used to forward commands to replica
 */
@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public class Replica {
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final AtomicLong lastCommunicated;
    private final BlockingQueue<String> cmdBuffer;
    private final String host;
    private final int port;
    Bootstrap b;
    private Channel channel;

    public Replica(String host, int port) {
        this.host = host;
        this.port = port;
        lastCommunicated = new AtomicLong(System.currentTimeMillis());
        cmdBuffer = new LinkedBlockingQueue<>();
    }

    public void init() {
        b = new Bootstrap();
        b.group(workerGroup)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .handler(new LoggingHandler(LogLevel.INFO))
         .handler(new StringCodecLineFrameInitializer());
    }

    public boolean connect() {
        if (channel != null) {
            return true;
        }
        if (b == null) {
            init();
        }
        int count = 0;
        while (count < 3) {
            final ChannelFuture future = b.connect(host, port).awaitUninterruptibly();
            if (future.isSuccess()) {
                channel = future.channel();
                return true;
            } else {
                count++;
            }
        }
        return false;
    }

    public Promise<Object> send(String msg) {
        final Promise<Object> resPromise = channel.eventLoop().newPromise();
        if (!connect()) {
            return resPromise.setFailure(new IOException("Lost connection to slave"));
        }
        channel.pipeline().addLast("replicaHandler", new ReplicaHandler(resPromise));
        channel.write(msg);
        channel.writeAndFlush("\n");
        return resPromise;
    }

    public CompletableFuture<Void> startWorker() {
        final String threadName = "repl-" + host + ':' + port + "-worker";
        final CompletableFuture<Void> stopFuture = new CompletableFuture<>();
        final Thread slaveWorker = new Thread(() -> {
            int count = 0;
            while (count < 3) {
                try {
                    final String line = getCmdBuffer().take();
                    final Promise<Object> send = send(line);
                    send.await(10000);
                    if (send.isSuccess()) {
                        final long now = System.currentTimeMillis();
                        getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
                        count = 0;
                    }
                } catch (Exception e) {
                    log.error("Failed to forward command: ", e);
                    count++;
                    Thread.currentThread().interrupt();
                }
            }
            stopFuture.complete(null);
        }, threadName);
        slaveWorker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(slaveWorker::interrupt));
        return stopFuture;
    }

    public void buffer(String line) {
        getCmdBuffer().add(line);
    }
}
