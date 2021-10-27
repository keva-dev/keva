package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.BulkReply;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

public class Info implements CommandHandler {
    @Override
    public BulkReply handle(List<String> args) {
        final HashMap<String, Object> stats = new HashMap<>();
        final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("threads", threads);
        return new BulkReply(stats.toString());
    }
}
