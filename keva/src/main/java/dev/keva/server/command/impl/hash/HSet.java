package dev.keva.server.command.impl.hash;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.Mutate;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("hset")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 3)
@Mutate
public class HSet {
    private final KevaDatabase database;

    @Autowired
    public HSet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[][] params) {
        String[] args = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                args[i] = null;
            } else {
                args[i] = new String(params[i]);
            }
        }
        if (args[0] == null) {
            return new IntegerReply(0);
        }
        int count = 0;
        for (int i = 1; i < args.length; i += 2) {
            if (args[i] != null) {
                database.hset(args[0].getBytes(), args[i].getBytes(), args[i + 1].getBytes());
                count++;
            }
        }
        return new IntegerReply(count);
    }
}
