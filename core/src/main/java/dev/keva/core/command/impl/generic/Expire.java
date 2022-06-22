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
@CommandImpl("expire")
@ParamLength(2)
public class Expire {
    private final KevaDatabase database;

    @Autowired
    public Expire(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] after) {
        try {
            long afterInSeconds = Long.parseLong(new String(after, StandardCharsets.UTF_8));
            database.setExpiration(key, System.currentTimeMillis() + afterInSeconds * 1000);
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }

}
