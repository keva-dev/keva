package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesValue;

import java.util.LinkedList;

@Component
@CommandImpl("llen")
@ParamLength(1)
public class LLen extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LLen(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return new IntegerReply(0);
        }
        LinkedList<BytesValue> list = this.getList(key);
        return new IntegerReply(list.size());
    }
}
