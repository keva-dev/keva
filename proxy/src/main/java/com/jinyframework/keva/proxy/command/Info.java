package com.jinyframework.keva.proxy.command;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

import com.jinyframework.keva.proxy.ServiceInstance;
import io.netty.channel.ChannelHandlerContext;

public class Info implements CommandHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, String line) {
        final HashMap<String, Object> stats = new HashMap<>();
        final long currentConnectedClients = ServiceInstance.getConnectionService().getCurrentConnectedClients();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients:", currentConnectedClients);
        stats.put("threads:", threads);
        ctx.write(stats.toString());
        ctx.flush();
    }
}
