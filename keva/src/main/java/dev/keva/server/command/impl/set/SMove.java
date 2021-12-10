package dev.keva.server.command.impl.set;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.Mutate;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("smove")
@ParamLength(3)
@Mutate
public class SMove {
    private final KevaDatabase database;

    @Autowired
    public SMove(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] source, byte[] destination, byte[] member) {
        int count = database.smove(source, destination, member);
        return new IntegerReply(count);
    }
}
