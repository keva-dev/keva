package dev.keva.core.command.impl.server;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;

@Component
@CommandImpl("time")
@ParamLength(0)
public class Time {
    @Execute
    public MultiBulkReply execute(byte[] ignored) {
        BulkReply[] replies = new BulkReply[1];
        replies[0] = new BulkReply(Long.toString(System.currentTimeMillis() / 1000));
        return new MultiBulkReply(replies);
    }
}
