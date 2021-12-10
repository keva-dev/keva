package dev.keva.core.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("del")
@ParamLength(type = AT_LEAST, value = 1)
@Mutate
public class Del {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public Del(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public IntegerReply execute(byte[]... keys) {
        var deleted = 0;
        for (byte[] key : keys) {
            if (database.remove(key)) {
                deleted++;
                expirationManager.clearExpiration(key);
            }
        }
        return new IntegerReply(deleted);
    }
}
