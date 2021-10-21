package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.protocol.redis.RedisCommandDecoder;
import com.jinyframework.keva.server.protocol.redis.RedisReplyEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class RedisCodecInitializer extends ChannelInitializer<SocketChannel> {
    private ChannelHandler handler;

    public RedisCodecInitializer(ChannelHandler handler) {
        this.handler = handler;
    }

    public RedisCodecInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();

        p.addLast(new RedisCommandDecoder());
        p.addLast(new RedisReplyEncoder());

        if (handler != null) {
            p.addLast(handler);
        }
    }
}
