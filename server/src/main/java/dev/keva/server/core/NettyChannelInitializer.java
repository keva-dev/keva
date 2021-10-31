package dev.keva.server.core;

import dev.keva.protocol.resp.RedisCommandDecoder;
import dev.keva.protocol.resp.RedisReplyEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.NonNull;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ChannelHandler handler;

    public NettyChannelInitializer(@NonNull ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new IdleStateHandler(0, 0, 5 * 60))
                .addLast(new RedisCommandDecoder())
                .addLast(new RedisReplyEncoder())
                .addLast(handler);
    }
}
