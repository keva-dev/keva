package com.jinyframework.keva.proxy.command;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface CommandHandler {
	void handle(ChannelHandlerContext ctx, String line);
}
