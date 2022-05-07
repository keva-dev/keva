package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.store.KevaDatabase;

import java.util.Arrays;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("msetnx")
@ParamLength(type = AT_LEAST, value = 2)
public class MSetNX {
    private final KevaDatabase database;

    @Autowired
    public MSetNX(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[]... params) {
        if (params.length % 2 != 0) {
            throw new CommandException("Wrong number of arguments for MSET");
        }

        byte[][] keys = new byte[params.length / 2][];
        for (int i = 0; i < params.length; i += 2) {
            keys[i / 2] = params[i];
        }
        byte[][] gets = database.mget(keys);

        if (Arrays.stream(gets).anyMatch(get -> get != null)) {
            return new IntegerReply(0);
        }

        database.mset(params);
        return new IntegerReply(1);
    }
}
