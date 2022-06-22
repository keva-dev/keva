package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("lrem")
@ParamLength(3)
@Mutate
public class LRem extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LRem(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] count, byte[] value) {
        int result = this.remove(key, Integer.parseInt(new String(count)), value);
        return new IntegerReply(result);
    }
}
