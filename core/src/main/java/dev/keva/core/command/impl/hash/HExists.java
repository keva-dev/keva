package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("hexists")
@ParamLength(2)
public class HExists extends HashBase {
    private final KevaDatabase database;

    @Autowired
    public HExists(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] field) {
        byte[] got = this.get(key, field);
        return got == null ? new IntegerReply(0) : new IntegerReply(1);
    }
}
