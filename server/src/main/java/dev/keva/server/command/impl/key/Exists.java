package dev.keva.server.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;

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
        var exists = 0;
        for (byte[] key : keys) {
            if (database.get(key) != null) {
                exists++;
            }
        }
        return new IntegerReply(exists);
    }
}
