package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("del")
@ParamLength(type = AT_LEAST, value = 1)
@Mutate
public class Del {
    private final KevaDatabase database;

    @Autowired
    public Del(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[]... keys) {
        long deleted = 0;
        for (byte[] key : keys) {
            if (database.remove(key)) {
                deleted++;
            }
        }
        return new IntegerReply(deleted);
    }
}
