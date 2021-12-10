package dev.keva.core.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("rename")
@ParamLength(2)
@Mutate
public class Rename {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public Rename(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public StatusReply execute(byte[] key, byte[] newName) {
        byte[] keyValue = database.get(key);
        if (keyValue == null) {
            return new StatusReply("ERR unknown key");
        }
        database.put(newName, keyValue);
        database.remove(key);
        expirationManager.move(key, newName);
        return StatusReply.OK;
    }
}
