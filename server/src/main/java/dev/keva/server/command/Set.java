package dev.keva.server.command;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.base.BaseCommandImpl;
import dev.keva.protocol.resp.reply.StatusReply;

@CommandImpl("set")
@ParamLength(2)
public class Set extends BaseCommandImpl {
    @Execute
    public StatusReply execute(byte[] key, byte[] val) {
        database.put(key, val);
        return new StatusReply("OK");
    }
}
