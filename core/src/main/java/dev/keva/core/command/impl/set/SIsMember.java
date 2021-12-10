package dev.keva.core.command.impl.set;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("sismember")
@ParamLength(2)
public class SIsMember {
    private final KevaDatabase database;

    @Autowired
    public SIsMember(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] value) {
        boolean isMember = database.sismember(key, value);
        return new IntegerReply(isMember ? 1 : 0);
    }
}
