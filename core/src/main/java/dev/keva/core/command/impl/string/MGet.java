package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("mget")
@ParamLength(type = AT_LEAST, value = 1)
public class MGet {
    private final KevaDatabase database;

    @Autowired
    public MGet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[]... keys) {
        BulkReply[] replies = new BulkReply[keys.length];
        byte[][] mget = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            byte[] key = keys[i];
            byte[] got = database.get(key);
            mget[i] = got;
        }
        for (int i = 0; i < mget.length; i++) {
            replies[i] = mget[i] == null ? BulkReply.NIL_REPLY : new BulkReply(mget[i]);
        }
        return new MultiBulkReply(replies);
    }
}
