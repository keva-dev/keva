package dev.keva.core.command.impl.string;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("setrange")
@ParamLength(type = EXACT, value = 3)
@Mutate
public class SetRange {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public SetRange(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] offset, byte[] val) {
        boolean isExpired = expirationManager.isExpirable(key);
        if (isExpired) {
            expirationManager.executeExpire(key);
        }
        return new IntegerReply(database.setrange(key, offset, val));
    }
}
