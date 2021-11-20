package dev.keva.server.command.impl.string;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("getdel")
@ParamLength(1)
public class GetDel {
    private final KevaDatabase database;

    @Autowired
    public GetDel(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key) {
        val got = database.get(key);
        if (got == null) {
            return BulkReply.NIL_REPLY;
        }
        database.remove(key);
        return new BulkReply(got);
    }
}