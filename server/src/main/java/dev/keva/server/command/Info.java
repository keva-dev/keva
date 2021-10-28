package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.resp.reply.BulkReply;
import lombok.val;

import java.lang.management.ManagementFactory;
import java.util.List;

public class Info implements CommandHandler {
    @Override
    public BulkReply handle(List<String> args) {
        val threads = ManagementFactory.getThreadMXBean().getThreadCount();
        String infoStr = "# Server\r\n" +
                "keva_version: 1.0.0\r\n"
                + "io_threads_active: " + threads;
        return new BulkReply(infoStr);
    }
}
