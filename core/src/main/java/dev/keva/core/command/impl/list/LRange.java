package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("lrange")
@ParamLength(3)
public class LRange extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LRange(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key, byte[] start, byte[] stop) {
        byte[][] got = this.range(key, Integer.parseInt(new String(start)), Integer.parseInt(new String(stop)));
        if (got == null) {
            return MultiBulkReply.EMPTY;
        }
        BulkReply[] replies = new BulkReply[got.length];
        for (int i = 0; i < got.length; i++) {
            replies[i] = new BulkReply(got[i]);
        }
        return new MultiBulkReply(replies);
    }
}
