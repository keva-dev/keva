package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("lset")
@ParamLength(3)
@Mutate
public class LSet extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LSet(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[] key, byte[] index, byte[] value) {
        this.set(key, Integer.parseInt(new String(index)), value);
        return StatusReply.OK;
    }
}
