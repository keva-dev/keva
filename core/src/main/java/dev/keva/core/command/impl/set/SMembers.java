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
@CommandImpl("smembers")
@ParamLength(1)
public class SMembers extends SetBase {
    private final KevaDatabase database;

    @Autowired
    public SMembers(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[] key) {
        byte[][] result = this.members(key);
        if (result == null) {
            return MultiBulkReply.EMPTY;
        }
        BulkReply[] replies = new BulkReply[result.length];
        for (int i = 0; i < result.length; i++) {
            replies[i] = new BulkReply(result[i]);
        }
        return new MultiBulkReply(replies);
    }
}
