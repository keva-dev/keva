package com.jinyframework.keva.server.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * A TCP client used to forward commands to replica
 */
public class ReplicaClient {
    private final String host;
    private final int port;
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public ReplicaClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ChannelFuture connect() {
        final Bootstrap b = new Bootstrap();
        b.group(workerGroup)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .handler(new LoggingHandler(LogLevel.INFO))
         .handler(new ClientChannelInitializer());

        return b.connect(host, port);
    }
}
