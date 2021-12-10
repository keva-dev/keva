package dev.keva.core.command.impl.set;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

import java.util.Arrays;

@Component
@CommandImpl("srem")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
@Mutate
public class SRem {
    private final KevaDatabase database;

    @Autowired
    public SRem(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[][] params) {
        int removed = database.srem(params[0], Arrays.copyOfRange(params, 1, params.length));
        return new IntegerReply(removed);
    }
}
