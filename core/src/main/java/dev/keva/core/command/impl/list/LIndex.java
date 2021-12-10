package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("lindex")
@ParamLength(2)
public class LIndex {
    private final KevaDatabase database;

    @Autowired
    public LIndex(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] index) {
        val got = database.lindex(key, Integer.parseInt(new String(index)));
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
