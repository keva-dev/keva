package dev.keva.server.command;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("del")
@ParamLength(type = AT_LEAST, value = 1)
public class Del {
    @Autowired
    private KevaDatabase database;

    @Execute
    public IntegerReply execute(byte[]... keys) {
        var deleted = 0;
        for (byte[] key : keys) {
            if (database.remove(key)) {
                deleted++;
            }
        }
        return new IntegerReply(deleted);
    }
}
