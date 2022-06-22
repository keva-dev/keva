package dev.keva.core.command.impl.set;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("sunion")
@ParamLength(2)
public class SUnion extends SetBase {
    private final KevaDatabase database;

    @Autowired
    public SUnion(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[]... keys) {
        byte[][] diff = this.union(keys);
        BulkReply[] replies = new BulkReply[diff.length];
        for (int i = 0; i < diff.length; i++) {
            replies[i] = new BulkReply(diff[i]);
        }
        return new MultiBulkReply(replies);
    }
}
