package dev.keva.core.command.impl.set;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("scard")
@ParamLength(1)
public class SCard {
    private final KevaDatabase database;

    @Autowired
    public SCard(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        int num = database.scard(key);
        return new IntegerReply(num);
    }
}
