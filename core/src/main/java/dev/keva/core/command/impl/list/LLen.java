package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("llen")
@ParamLength(1)
public class LLen {
    private final KevaDatabase database;

    @Autowired
    public LLen(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] count) {
        int got = database.llen(key);
        return new IntegerReply(got);
    }
}
