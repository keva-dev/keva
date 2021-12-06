package dev.keva.server.command.impl.list;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("lrange")
@ParamLength(3)
public class LRange {
    private final KevaDatabase database;

    @Autowired
    public LRange(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key, byte[] start, byte[] stop) {
        val got = database.lrange(key, Integer.parseInt(new String(start)), Integer.parseInt(new String(stop)));
        if (got == null) {
            return new MultiBulkReply(new BulkReply[0]);
        }
        BulkReply[] replies = new BulkReply[got.length];
        for (int i = 0; i < got.length; i++) {
            replies[i] = new BulkReply(got[i]);
        }
        return new MultiBulkReply(replies);
    }
}
