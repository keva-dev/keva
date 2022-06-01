package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

import java.math.BigInteger;
import java.util.Base64;

@Component
@CommandImpl("restore")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 3)
public class Restore {
    private final KevaDatabase database;

    @Autowired
    public Restore(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key, byte[] ttl, byte[] dump, byte[] replace) {
        byte[] old = database.get(key);
        boolean isReplace = replace != null && new String(replace).equalsIgnoreCase("REPLACE");
        if (old != null && !isReplace) {
            return new ErrorReply("ERR Target key name is busy");
        }
        byte[] value = Base64.getDecoder().decode(dump);
        database.put(key, value);
        long expireTime = new BigInteger(ttl).longValue();
        if (expireTime > 0) {
            database.setExpiration(key, System.currentTimeMillis() + expireTime * 1000);
        }
        return StatusReply.OK;
    }
}
