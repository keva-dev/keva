package dev.keva.server.command.impl.connection;

import dev.keva.ioc.annotation.Component;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;

import dev.keva.protocol.resp.reply.BulkReply;
import lombok.val;

import java.lang.management.ManagementFactory;

@Component
@CommandImpl("info")
@ParamLength(0)
public class Info {
    @Execute
    public BulkReply execute() {
        val threads = ManagementFactory.getThreadMXBean().getThreadCount();
        String infoStr = "# Server\r\n" +
                "keva_version: 1.0.0\r\n"
                + "io_threads_active: " + threads + "\r\n";
        return new BulkReply(infoStr);
    }
}
