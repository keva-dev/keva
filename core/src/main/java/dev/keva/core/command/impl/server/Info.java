package dev.keva.core.command.impl.server;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;

import java.lang.management.ManagementFactory;

@Component
@CommandImpl("info")
@ParamLength(0)
public class Info {
    @Execute
    public BulkReply execute() {
        int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        String infoStr = "# Server\r\n" +
                "keva_version: 1.0.0\r\n"
                + "io_threads_active: " + threads + "\r\n";
        return new BulkReply(infoStr);
    }
}
