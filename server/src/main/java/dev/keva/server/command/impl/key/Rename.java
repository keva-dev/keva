package dev.keva.server.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.key.manager.ExpirationManager;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("rename")
@ParamLength(2)
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
