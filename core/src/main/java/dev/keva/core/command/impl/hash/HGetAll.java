package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("hgetall")
@ParamLength(1)
public class HGetAll extends HashBase {
    private final KevaDatabase database;

    @Autowired
    public HGetAll(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[] key) {
        byte[][] got = this.getAll(key);
        BulkReply[] replies = new BulkReply[got.length];
        for (int i = 0; i < got.length; i++) {
            replies[i] = new BulkReply(got[i]);
        }
        return new MultiBulkReply(replies);
    }
}
