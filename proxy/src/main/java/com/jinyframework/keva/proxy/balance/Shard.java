package com.jinyframework.keva.proxy.balance;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.jinyframework.keva.proxy.core.StringCodecLineFrameInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
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
	private final Queue<Request> cmdBuffer;
	private final AtomicBoolean isAlive = new AtomicBoolean(true);
	private final ExecutorService workerPool = Executors.newFixedThreadPool(1);

	public Shard(String host, int port) {
		this.host = host;
		this.port = port;
		cmdBuffer = new LinkedList<>();
		lastCommunicated = new AtomicLong(System.currentTimeMillis());
		workerPool.submit(this.commandRelayTask());
	}

	public void init() {
		b = new Bootstrap();
		b.group(workerGroup)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new LoggingHandler(LogLevel.INFO))
			.handler(new StringCodecLineFrameInitializer());
		log.info("Init new shard {}:{}", host, port);
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
			isAlive.getAndSet(true);
			return true;
		}
		isAlive.getAndSet(false);
		return false;
	}

	public Runnable commandRelayTask() {
		return () -> {
			Request request;
			while (isAlive.get()) {
				try {
					request = getCmdBuffer().poll();
					if (request != null) {
						String res = (String)this.send(request.getRequestContent()).get();
						ChannelHandlerContext context = request.getChannelContext();
						context.writeAndFlush(res);
					}
				} catch (Exception e) {
					log.trace("Failed to forward command: ", e);
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
				}
			}
		};
	}

	public void buffer(Request request) {
		cmdBuffer.add(request);
	}

	private CompletableFuture<Object> send(String msg) {
		final CompletableFuture<Object> resPromise = new CompletableFuture<>();
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
