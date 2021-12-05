package dev.keva.server.command.impl.list;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

@Component
@CommandImpl("lrem")
@ParamLength(3)
public class LRem {
    private final KevaDatabase database;

    @Autowired
    public LRem(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] count, byte[] value) {
        int result = database.lrem(key, Integer.parseInt(new String(count)), value);
        return new IntegerReply(result);
    }
}
