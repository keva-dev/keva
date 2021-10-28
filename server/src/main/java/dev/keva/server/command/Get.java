package dev.keva.server.command;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.base.BaseCommandImpl;
import dev.keva.server.protocol.resp.reply.BulkReply;
import dev.keva.server.protocol.resp.reply.Reply;
import lombok.val;

@CommandImpl("get")
@ParamLength(1)
public class Get extends BaseCommandImpl {
    @Execute
    public Reply<?> execute(byte[] key) {
        val got = database.get(key);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
