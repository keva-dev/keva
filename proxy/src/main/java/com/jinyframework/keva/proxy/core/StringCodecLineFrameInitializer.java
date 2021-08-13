package com.jinyframework.keva.proxy.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class StringCodecLineFrameInitializer extends ChannelInitializer<SocketChannel> {
    private static final StringDecoder DECODER = new StringDecoder(CharsetUtil.UTF_8);
    private static final StringEncoder ENCODER = new StringEncoder(CharsetUtil.UTF_8);

    private ChannelHandler handler;

    public StringCodecLineFrameInitializer(ChannelHandler handler) {
        this.handler = handler;
    }

    public StringCodecLineFrameInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.config().setKeepAlive(true);
        final ChannelPipeline pipeline = ch.pipeline();

        // Add the text line codec combination first,
        final int maxFrameLength = 1024 * 1024 * 64; // hardcode 64MB for now
        pipeline.addLast(new DelimiterBasedFrameDecoder(maxFrameLength, Delimiters.lineDelimiter()));

        pipeline.addLast(DECODER);
        pipeline.addLast(ENCODER);

        // and then business logic.
        if (handler != null) {
            pipeline.addLast(handler);
        }
    }
}
