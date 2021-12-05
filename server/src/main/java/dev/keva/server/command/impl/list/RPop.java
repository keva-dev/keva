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
@CommandImpl("rpop")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 1)
public class RPop {
    private final KevaDatabase database;

    @Autowired
    public RPop(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key, byte[] count) {
        if (count == null) {
            val got = database.rpop(key);
            return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
        }

        int countInt = Integer.parseInt(new String(count));
        Reply<?>[] replies = new Reply[countInt];
        for (int i = 0; i < countInt; i++) {
            val got = database.rpop(key);
            replies[i] = got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
        }
        return new MultiBulkReply(replies);
    }
}
