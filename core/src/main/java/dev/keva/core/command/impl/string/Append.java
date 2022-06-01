package dev.keva.core.command.impl.string;

import com.google.common.primitives.Bytes;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("append")
@ParamLength(2)
@Mutate
public class Append {
    private final KevaDatabase database;

    @Autowired
    public Append(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] val) {
        byte[] currentValue = database.get(key);
        long length = 0;
        if (currentValue == null) {
            database.put(key, val);
            length = val.length;
        } else {
            database.put(key, Bytes.concat(currentValue, val));
            length = currentValue.length + val.length;
        }
        return new IntegerReply(length);
    }
}
