package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("get")
@ParamLength(1)
public class Get {
    private final KevaDatabase database;

    @Autowired
    public Get(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key) {
        byte[] got = database.get(key);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
