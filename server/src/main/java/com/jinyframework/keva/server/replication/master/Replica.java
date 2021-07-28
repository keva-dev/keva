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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A TCP client used to forward commands to replica
 */
@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public class Replica {
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    private final AtomicLong lastCommunicated;
    private final BlockingQueue<String> cmdBuffer;
    private final String host;
    private final int port;
    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    Bootstrap b;
    private Channel channel;

    public Replica(String host, int port) {
        this.host = host;
        this.port = port;
        lastCommunicated = new AtomicLong(System.currentTimeMillis());
        cmdBuffer = new LinkedBlockingQueue<>();
        init();
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

    public boolean alive() {
        return isAlive.get();
    }

    public void connect() {
        int retries = 0;
        final int maxRetries = 3;
        final int timeoutInterval = 300; // millisecond
        while (retries < maxRetries) {
            final ChannelFuture channelFuture = b.connect(host, port).awaitUninterruptibly();
            if (channelFuture.isSuccess()) {
                channel = channelFuture.channel();
                isAlive.getAndSet(true);
                return;
            }
            retries++;
            try {
                TimeUnit.MILLISECONDS.sleep(timeoutInterval);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public CompletableFuture<Object> send(String msg) {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        channel.pipeline().addLast(new ReplicaHandler(future));
        channel.write(msg);
        channel.writeAndFlush("\n");
        return future;
    }

    public Runnable commandRelayTask() {
        return () -> {
            String lastFailedLine = null;
            String line = null;
            while (isAlive.get()) {
                try {
                    line = lastFailedLine == null ? getCmdBuffer().take() : lastFailedLine;
                    log.info(line);
                    final CompletableFuture<Object> send = send(line);
                    send.get(3000, TimeUnit.MILLISECONDS);
                    final long now = System.currentTimeMillis();
                    getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
                    lastFailedLine = null;
                } catch (Exception e) {
                    lastFailedLine = line;
                    log.warn("Failed to forward command: ", e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }

    public void buffer(String line) {
        getCmdBuffer().add(line);
    }

    public ScheduledFuture<?> startHealthChecker(ScheduledExecutorService healthCheckerPool,
                                                 CompletableFuture<Object> lost) {
        final AtomicInteger retries = new AtomicInteger(0);
        final int maxRetry = 3;
        return healthCheckerPool.scheduleAtFixedRate(() -> {
            // ping and update
            final CompletableFuture<Object> ping = send("PING");
            try {
                final String pong = (String) ping.get(300, TimeUnit.MILLISECONDS);
                if ("PONG".equalsIgnoreCase(pong)) {
                    retries.getAndSet(0);
                    final long now = System.currentTimeMillis();
                    getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
                } else {
                    throw new Exception("Incorrect slave result: " + pong);
                }
            } catch (Exception e) {
                log.warn("Ping failed", e);
                retries.getAndIncrement();
                if (retries.get() >= maxRetry) {
                    isAlive.getAndSet(false);
                    lost.complete(true);
                }
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }, 5, 1, TimeUnit.SECONDS);
    }
}
