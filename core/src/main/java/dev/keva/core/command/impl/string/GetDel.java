package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("getdel")
@ParamLength(1)
@Mutate
public class GetDel {
    private final KevaDatabase database;

    @Autowired
    public GetDel(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key) {
        byte[] got = database.get(key);
        if (got == null) {
            return BulkReply.NIL_REPLY;
        }
        database.remove(key);
        return new BulkReply(got);
    }
}
