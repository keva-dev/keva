package dev.keva.server.command.impl.string;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("mget")
@ParamLength(type = AT_LEAST, value = 1)
public class MGet {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public MGet(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public MultiBulkReply execute(byte[]... keys) {
        BulkReply[] replies = new BulkReply[keys.length];
        byte[][] mget = database.mget(keys);
        for (int i = 0; i < mget.length; i++) {
            replies[i] = mget[i] == null ? BulkReply.NIL_REPLY : new BulkReply(mget[i]);
        }
        return new MultiBulkReply(replies);
    }
}
