package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("rename")
@ParamLength(2)
@Mutate
public class Rename {
    private final KevaDatabase database;

    @Autowired
    public Rename(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[] key, byte[] newName) {
        boolean success = database.rename(key, newName);
        if (success) {
            return StatusReply.OK;
        } else {
            return new StatusReply("ERR unknown key");
        }
    }
}
