package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("hkeys")
@ParamLength(1)
public class HKeys {
    private final KevaDatabase database;

    @Autowired
    public HKeys(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[] key) {
        byte[][] got = database.hkeys(key);
        BulkReply[] replies = new BulkReply[got.length];
        for (int i = 0; i < got.length; i++) {
            replies[i] = new BulkReply(got[i]);
        }
        return new MultiBulkReply(replies);
    }
}
