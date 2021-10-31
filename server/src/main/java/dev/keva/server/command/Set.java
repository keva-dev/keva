package dev.keva.server.command;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("set")
@ParamLength(2)
public class Set {
    @Autowired
    protected static KevaDatabase database;

    @Execute
    public StatusReply execute(byte[] key, byte[] val) {
        database.put(key, val);
        return new StatusReply("OK");
    }
}
