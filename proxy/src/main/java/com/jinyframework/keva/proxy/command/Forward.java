package com.jinyframework.keva.proxy.command;

import java.util.List;

import com.jinyframework.keva.proxy.ServiceInstance;
import command.CommandHandler;

public class Forward implements CommandHandler {
	@Override
	public Object handle(List<String> args) {
		String request = CommandService.parseLine(args);
		return ServiceInstance.getLoadBalancingService().forwardRequest(request, args.get(1));
	}
}
