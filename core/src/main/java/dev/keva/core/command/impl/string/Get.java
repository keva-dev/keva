package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("get")
@ParamLength(1)
public class Get {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public Get(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public Reply<?> execute(byte[] key) {
        boolean expirable = expirationManager.isExpirable(key);
        if (expirable) {
            expirationManager.executeExpire(key);
            return BulkReply.NIL_REPLY;
        } else {
            val got = database.get(key);
            return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
        }
    }
}
