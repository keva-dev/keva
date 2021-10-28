package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.resp.reply.BulkReply;
import lombok.val;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Info implements CommandHandler {
    @Override
    public BulkReply handle(List<String> args) {
        final Map<String, Object> stats = new HashMap<>();
        val threads = ManagementFactory.getThreadMXBean().getThreadCount();
        stats.put("threads", threads);
        return new BulkReply(stats.toString());
    }
}
