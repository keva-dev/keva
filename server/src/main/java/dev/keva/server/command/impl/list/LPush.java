package dev.keva.server.command.impl.list;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

import java.util.Arrays;

@Component
@CommandImpl("lpush")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
public class LPush {
    private final KevaDatabase database;

    @Autowired
    public LPush(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[]... params) {
        int count = database.lpush(params[0], Arrays.copyOfRange(params, 1, params.length));
        return new IntegerReply(count);
    }
}
