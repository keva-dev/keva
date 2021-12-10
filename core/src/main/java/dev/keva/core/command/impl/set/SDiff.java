package dev.keva.core.command.impl.set;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("sdiff")
@ParamLength(2)
public class SDiff {
    private final KevaDatabase database;

    @Autowired
    public SDiff(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[]... keys) {
        byte[][] diff = database.sdiff(keys);
        BulkReply[] replies = new BulkReply[diff.length];
        for (int i = 0; i < diff.length; i++) {
            replies[i] = new BulkReply(diff[i]);
        }
        return new MultiBulkReply(replies);
    }
}
