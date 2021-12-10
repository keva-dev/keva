package dev.keva.core.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;
import lombok.val;

import java.math.BigInteger;
import java.util.Base64;

@Component
@CommandImpl("restore")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 3)
public class Restore {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public Restore(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public Reply<?> execute(byte[] key, byte[] ttl, byte[] dump, byte[] replace) {
        val old = database.get(key);
        boolean isReplace = replace != null && new String(replace).equalsIgnoreCase("REPLACE");
        if (old != null && !isReplace) {
            return new ErrorReply("ERR Target key name is busy");
        }
        byte[] value = Base64.getDecoder().decode(dump);
        database.put(key, value);
        long expireTime = new BigInteger(ttl).longValue();
        if (expireTime > 0) {
            expirationManager.expireAfter(key, expireTime);
        }
        return StatusReply.OK;
    }
}
