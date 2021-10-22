package dev.keva.server.core;

import dev.keva.server.protocol.redis.RedisCommandDecoder;
import dev.keva.server.protocol.redis.RedisReplyEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class RedisCodecInitializer extends ChannelInitializer<SocketChannel> {
    private ChannelHandler handler;

    public RedisCodecInitializer(ChannelHandler handler) {
        this.handler = handler;
    }

    public RedisCodecInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();

        p.addLast(new IdleStateHandler(0, 0, 5 * 60));
        p.addLast(new RedisCommandDecoder());
        p.addLast(new RedisReplyEncoder());

        if (handler != null) {
            p.addLast(handler);
        }
    }
}
