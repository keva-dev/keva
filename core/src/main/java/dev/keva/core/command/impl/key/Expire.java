package dev.keva.core.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("expire")
@ParamLength(2)
public class Expire {
    private final ExpirationManager expirationManager;

    @Autowired
    public Expire(ExpirationManager expirationManager) {
        this.expirationManager = expirationManager;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] after) {
        try {
            var afterInMillis = Long.parseLong(new String(after, StandardCharsets.UTF_8));
            expirationManager.expireAfter(key, afterInMillis);
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }

}
