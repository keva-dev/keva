package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("expireat")
@ParamLength(2)
public class ExpireAt {
    private final KevaDatabase database;

    @Autowired
    public ExpireAt(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] at) {
        try {
            long atInMillis = Long.parseLong(new String(at, StandardCharsets.UTF_8));
            database.setExpiration(key, atInMillis);
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }

}
