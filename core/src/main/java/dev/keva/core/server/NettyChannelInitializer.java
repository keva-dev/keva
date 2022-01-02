package dev.keva.core.server;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.RedisCommandDecoder;
import dev.keva.protocol.resp.RedisReplyEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;


@Component
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final NettyChannelHandler handler;

    @Autowired
    public NettyChannelInitializer(NettyChannelHandler handler) {
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
