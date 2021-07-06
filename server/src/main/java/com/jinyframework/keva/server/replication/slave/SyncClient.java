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
    private final String masterHost;
    private final int masterPort;
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
        final ChannelFuture future = b.connect(masterHost, masterPort).awaitUninterruptibly();
        if (future.isSuccess()) {
            channel = future.channel();
            return true;
        } else {
            return false;
        }
    }

    public Promise<Object> fullSync(String slaveHost, Integer slavePort) {
        final ChannelFuture fsync = channel.writeAndFlush("FSYNC " + slaveHost + ' ' + slavePort + '\n');
        final Promise<Object> resPromise = channel.eventLoop().newPromise();
        fsync.channel().pipeline().addLast(new SyncHandler(resPromise));
        return resPromise;
    }
}
