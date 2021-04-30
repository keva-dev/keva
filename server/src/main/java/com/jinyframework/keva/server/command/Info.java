package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceFactory;
import lombok.val;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

public class Info implements CommandHandler {
    @Override
    public Object handle(List<String> args) {
        val stats = new HashMap<String, Object>();
        val currentConnectedClients = ServiceFactory.getConnectionService().getCurrentConnectedClients();
        val threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("clients:", currentConnectedClients);
        stats.put("threads:", threads);
        return stats;
    }
}
