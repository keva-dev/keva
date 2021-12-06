package dev.keva.server.command.impl.string;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

import static dev.keva.server.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("setrange")
@ParamLength(type = EXACT, value = 3)
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
