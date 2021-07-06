package com.jinyframework.keva.server.replication.master;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
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
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();
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
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) throws Exception {
                 ch.config().setKeepAlive(true);
                 final ChannelPipeline pipeline = ch.pipeline();

                 // Add the text line codec combination first,
                 final int maxFrameLength = 1024 * 1024 * 64; // hardcode 64MB for now
                 pipeline.addLast(new DelimiterBasedFrameDecoder(maxFrameLength, Delimiters.lineDelimiter()));
                 // the encoder and decoder are static as these are sharable
                 pipeline.addLast(DECODER);
                 pipeline.addLast(ENCODER);
             }
         });
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

    public Promise<Object> send(String msg) throws Exception {
        if (!connect()) {
            throw new Exception("Lost connection to slave");
        }
        final Promise<Object> resPromise= channel.eventLoop().newPromise();
        channel.pipeline().addLast("replicaHandler",new ReplicaHandler(resPromise));
        channel.write(msg);
        channel.writeAndFlush("\n");
        return resPromise;
    }

    public void startWorker() {
        final String threadName = "repl-" + host + ':' + port + "-worker";
        new Thread(() -> {
            while (true) {
                try {
                    final String line = getCmdBuffer().take();
                    final Promise<Object> send = send(line);
                    send.await(10000);
                    if (send.isSuccess()) {
                        final long now = System.currentTimeMillis();
                        getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
                    }
                } catch (Exception e) {
                    log.error("Failed to forward command: ", e);
                }
            }
        }, threadName).start();
    }

    public void buffer(String line) {
        getCmdBuffer().add(line);
    }
}
