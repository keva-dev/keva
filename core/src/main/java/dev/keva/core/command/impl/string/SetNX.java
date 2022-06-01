package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("setnx")
@ParamLength(type = EXACT, value = 2)
public class SetNX {
    private final KevaDatabase database;

    @Autowired
    public SetNX(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[]... params) {
        byte[] get = database.get(params[0]);
        if(get != null) {
            return new IntegerReply(0);
        }
        database.put(params[0], params[1]);
        return new IntegerReply(1);
    }
}
