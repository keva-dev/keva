package dev.keva.core.command.impl.hash;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("hlen")
@ParamLength(1)
public class HLen {
    private final KevaDatabase database;

    @Autowired
    public HLen(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        val got = database.hgetAll(key);
        return got == null ? new IntegerReply(0) : new IntegerReply(got.length / 2);
    }
}
