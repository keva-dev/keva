package dev.keva.server.cluster;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class KevaNode {

    private final String host;
    private final int port;
    Bootstrap b;
    private Channel channel;

    public KevaNode(String host, String port) {
        this.host = host;
        this.port = Integer.parseInt(port);
    }

    public void init() {
        b = new Bootstrap();
        b.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class);
    }

    public boolean connect() {
        if (channel != null) {
            return true;
        }
        if (b == null) {
            init();
        }

        final ChannelFuture future = b.connect(host, port).awaitUninterruptibly();
        if (future.isSuccess()) {
            channel = future.channel();
            channel.pipeline().addLast(new ByteArrayDecoder());
            channel.pipeline().addLast(new ByteArrayDecoder());
            return true;
        }
        return false;
    }

    public String send(byte[] msg) throws ExecutionException, InterruptedException {
        final CompletableFuture<Object> resPromise = new CompletableFuture<>();
        connect();
        channel.pipeline().addLast(new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                resPromise.complete(msg);
            }
        });
        channel.write(msg);
        channel.writeAndFlush("\n");
        return (String) resPromise.get();
    }
}
