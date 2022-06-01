package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import java.util.Arrays;

@Component
@CommandImpl("lpush")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
@Mutate
public class LPush extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LPush(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[]... params) {
        int count = this.lpush(params[0], Arrays.copyOfRange(params, 1, params.length));
        return new IntegerReply(count);
    }
}
