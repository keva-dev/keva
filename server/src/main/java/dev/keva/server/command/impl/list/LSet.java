package dev.keva.server.command.impl.list;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("lset")
@ParamLength(3)
public class LSet {
    private final KevaDatabase database;

    @Autowired
    public LSet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[] key, byte[] index, byte[] value) {
        database.lset(key, Integer.parseInt(new String(index)), value);
        return new StatusReply("OK");
    }
}
