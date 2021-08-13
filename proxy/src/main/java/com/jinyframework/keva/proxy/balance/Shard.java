package com.jinyframework.keva.proxy.balance;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import com.jinyframework.keva.proxy.core.StringCodecLineFrameInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Shard {
	private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
	private final AtomicLong lastCommunicated;
	private final String host;
	private final int port;
	Bootstrap b;
	private Channel channel;

	public Shard(String host, int port) {
		this.host = host;
		this.port = port;
		lastCommunicated = new AtomicLong(System.currentTimeMillis());
	}

	public void init() {
		b = new Bootstrap();
		b.group(workerGroup)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new LoggingHandler(LogLevel.INFO))
			.handler(new StringCodecLineFrameInitializer());
	}

	public boolean connect() {
		if (channel != null) {
			return true;
		}
		if (b == null) {
			init();
		}
		int count = 0;
		while (count < 3) {
			final ChannelFuture future = b.connect(host, port).awaitUninterruptibly();
			if (future.isSuccess()) {
				channel = future.channel();
				return true;
			} else {
				count++;
			}
		}
		return false;
	}

	public CompletableFuture<Object> send(String msg) {
		final CompletableFuture<Object> resPromise = new CompletableFuture<>();
		// lazy initialization, only try to connect when sending
		if (!connect()) {
			resPromise.completeExceptionally(new IOException("Lost connection to server"));
			return resPromise;
		}
		channel.pipeline().addLast(new ShardHandler(resPromise));
		channel.write(msg);
		channel.writeAndFlush("\n");
		return resPromise;
	}
}
