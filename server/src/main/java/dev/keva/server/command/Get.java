package dev.keva.server.command;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("get")
@ParamLength(1)
public class Get {
    @Autowired
    private KevaDatabase database;

    @Execute
    public Reply<?> execute(byte[] key) {
        val got = database.get(key);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
