package com.jinyframework.keva.proxy.command;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

import com.jinyframework.keva.proxy.ServiceInstance;
import com.jinyframework.keva.server.command.CommandHandler;

public class Info implements CommandHandler {
    @Override
    public String handle(List<String> args) {
        final HashMap<String, Object> stats = new HashMap<>();
        final long currentConnectedClients = ServiceInstance.getConnectionService().getCurrentConnectedClients();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients:", currentConnectedClients);
        stats.put("threads:", threads);
        return stats.toString();
    }
}
