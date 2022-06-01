package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("exists")
@ParamLength(type = AT_LEAST, value = 1)
public class Exists {
    private final KevaDatabase database;

    @Autowired
    public Exists(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[]... keys) {
        long exists = 0;
        for (byte[] key : keys) {
            if (database.get(key) != null) {
                exists++;
            }
        }
        return new IntegerReply(exists);
    }
}
