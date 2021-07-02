package com.jinyframework.keva.server.replication.slave;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Promise;

/**
 * A TCP client used to make request to master
 */
public class SyncClient {
    // Using StringDecoder will fail due to special character
    private static final ByteArrayDecoder DECODER = new ByteArrayDecoder();
    private static final StringEncoder ENCODER = new StringEncoder(CharsetUtil.UTF_8);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final String host;
    private final int port;
    private Channel channel;

    public SyncClient(String host, int port) {
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
                 final int maxFrameLength = 1024 * 1024 * 1024;
                 pipeline.addLast(new DelimiterBasedFrameDecoder(maxFrameLength, Delimiters.lineDelimiter()));
                 // the encoder and decoder are static as these are sharable
                 pipeline.addLast(DECODER);
                 pipeline.addLast(ENCODER);
             }
         });
        try {
            channel = b.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            return false;
        }

        return true;
    }

    public Promise<Object> fullSync(String host, int port) {
        final ChannelFuture fsync = channel.writeAndFlush("FSYNC " + host + ' ' + port + '\n');
        final Promise<Object> resPromise = channel.eventLoop().newPromise();
        fsync.channel().pipeline().addLast(new SyncHandler(resPromise));
        return resPromise;
    }
}
