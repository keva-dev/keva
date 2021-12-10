package dev.keva.server.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.key.manager.ExpirationManager;

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
