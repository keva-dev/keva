package com.jinyframework.keva.proxy.command;

import java.util.List;

import com.jinyframework.keva.proxy.ServiceInstance;
import com.jinyframework.keva.proxy.balance.Request;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

public class Forward implements CommandHandler {
	@Override
	public void handle(ChannelHandlerContext ctx, String line) {
		val args = CommandService.parseTokens(line);
		ServiceInstance.getLoadBalancingService().forwardRequest(new Request(ctx, line), args.get(1));
	}
}
