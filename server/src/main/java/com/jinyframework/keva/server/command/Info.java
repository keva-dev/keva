package com.jinyframework.keva.server.command;

import lombok.val;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

import static com.jinyframework.keva.server.ServiceFactory.connectionService;

public class Info implements CommandHandler {
    @Override
    public Object handle(List<String> args) {
        val stats = new HashMap<String, Object>();
        val currentConnectedClients = connectionService().getCurrentConnectedClients();
        val threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients:", currentConnectedClients);
        stats.put("threads:", threads);
        return stats;
    }
}
