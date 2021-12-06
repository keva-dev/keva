package dev.keva.server.command.impl.connection;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_MOST;

@Component
@CommandImpl("ping")
@ParamLength(type = AT_MOST, value = 1)
public class Ping {
    @Execute
    public Reply<?> execute(byte[] thing) {
        if (thing != null) {
            return new BulkReply(thing);
        }
        return new StatusReply("PONG");
    }
}
