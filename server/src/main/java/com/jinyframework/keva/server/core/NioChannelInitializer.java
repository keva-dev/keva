package com.jinyframework.keva.server.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NioChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();

    private static final NioServerHandler SERVER_HANDLER = new NioServerHandler();

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

        // and then business logic.
        pipeline.addLast(SERVER_HANDLER);
    }
}
