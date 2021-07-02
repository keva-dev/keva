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

/**
 * A TCP client used to forward commands to replica
 */
public class ReplicaClient {
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final String host;
    private final int port;
    private Channel channel;

    public ReplicaClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        final Bootstrap b = new Bootstrap();
        b.group(workerGroup)
         .channel(NioSocketChannel.class)
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
        try {
            channel = b.connect(host, port).sync().channel();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public Promise<Object> send(String msg) {
        final Promise<Object> resPromise = channel.eventLoop().newPromise();
        channel.pipeline().addLast(new ReplicaHandler(resPromise));
        channel.write(msg);
        channel.writeAndFlush("\n");
        return resPromise;
    }
}
