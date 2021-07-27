package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.core.StringCodecLineFrameInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;

/**
 * A TCP client used to make request to master
 */
public class SyncClient {
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
         .handler(new StringCodecLineFrameInitializer());
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
