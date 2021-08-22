package com.jinyframework.keva.server.replication.master;

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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A TCP client used to forward commands to replica
 */
@Getter
@ToString
@EqualsAndHashCode
@Slf4j
public class Replica {
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    private final long joinedTime;
    private final BlockingQueue<String> cmdBuffer;
    private final String host;
    private final int port;
    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    @Getter(AccessLevel.NONE)
    private final LinkedBlockingDeque<CompletableFuture<Object>> resFutureQueue = new LinkedBlockingDeque<>();
    @Getter(AccessLevel.NONE)
    private final FutureHandler futureHandler = new FutureHandler(resFutureQueue);
    @Getter(AccessLevel.NONE)
    private Bootstrap b;
    private Channel channel;

    public Replica(String host, int port) {
        this.host = host;
        this.port = port;
        cmdBuffer = new LinkedBlockingQueue<>();
        joinedTime = System.currentTimeMillis();
        init();
    }

    public void init() {
        b = new Bootstrap();
        b.group(workerGroup)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .handler(new LoggingHandler(LogLevel.INFO))
         .handler(new StringCodecLineFrameInitializer(futureHandler));
    }

    public boolean alive() {
        return isAlive.get();
    }

    public void connect() {
        int retries = 0;
        final int maxRetries = 3;
        final int timeoutInterval = 500; // millisecond
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
        if (resFutureQueue.offer(future)) {
            channel.write(msg);
            channel.writeAndFlush("\n");
        } else {
            future.completeExceptionally(new IllegalStateException("Protocol client queue failure"));
        }
        return future;
    }

    public Runnable commandRelayTask() {
        return () -> {
            String lastFailedLine = null;
            String line = null;
            while (alive()) {
                try {
                    line = lastFailedLine == null ? getCmdBuffer().take() : lastFailedLine;
                    log.trace(line);
                    final CompletableFuture<Object> send = send(line);
                    send.get(3000, TimeUnit.MILLISECONDS);
                    lastFailedLine = null;
                } catch (Exception e) {
                    lastFailedLine = line;
                    log.trace("Failed to forward command: ", e);
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

    public Runnable healthChecker(CompletableFuture<Object> lost) {
        final AtomicInteger retries = new AtomicInteger(0);
        final int maxRetry = 3;
        return () -> {
            // ping and update
            final CompletableFuture<Object> ping = send("PING");
            try {
                final String pong = (String) ping.get(300, TimeUnit.MILLISECONDS);
                if ("PONG".equalsIgnoreCase(pong)) {
                    retries.getAndSet(0);
                } else {
                    retries.getAndIncrement();
                }
            } catch (Exception e) {
                log.trace("Ping failed: ", e);
                retries.getAndIncrement();
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            if (retries.get() >= maxRetry) {
                isAlive.getAndSet(false);
                lost.complete(true);
                cmdBuffer.clear();
            }
        };
    }
}
