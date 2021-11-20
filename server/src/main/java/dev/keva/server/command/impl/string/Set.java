package dev.keva.server.command.impl.string;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("set")
@ParamLength(2)
public class Set {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public Set(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public StatusReply execute(byte[] key, byte[] val) {
        database.put(key, val);
        expirationManager.clearExpiration(key);
        return new StatusReply("OK");
    }
}
