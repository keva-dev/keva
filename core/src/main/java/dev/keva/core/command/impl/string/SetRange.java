package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("setrange")
@ParamLength(type = EXACT, value = 3)
@Mutate
public class SetRange {
    private final KevaDatabase database;

    @Autowired
    public SetRange(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] offset, byte[] val) {
        int offsetPosition = Integer.parseInt(new String(offset, StandardCharsets.UTF_8));
        byte[] oldVal = database.get(key);
        int newValLength = oldVal == null ? offsetPosition + val.length : Math.max(offsetPosition + val.length, oldVal.length);
        byte[] newVal = new byte[newValLength];
        for (int i = 0; i < newValLength; i++) {
            if (i >= offsetPosition && i < offsetPosition + val.length) {
                newVal[i] = val[i - offsetPosition];
            } else if (oldVal != null && i < oldVal.length) {
                newVal[i] = oldVal[i];
            } else {
                newVal[i] = 0b0;
            }
        }
        database.put(key, newVal);
        return new IntegerReply(newValLength);
    }
}
