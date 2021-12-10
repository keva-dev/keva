package dev.keva.core.command.impl.key;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("expireat")
@ParamLength(2)
public class ExpireAt {
    private final ExpirationManager expirationManager;

    @Autowired
    public ExpireAt(ExpirationManager expirationManager) {
        this.expirationManager = expirationManager;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] at) {
        try {
            var atInMillis = Long.parseLong(new String(at, StandardCharsets.UTF_8));
            expirationManager.expireAt(key, atInMillis);
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }

}
