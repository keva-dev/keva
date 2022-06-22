package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("strlen")
@ParamLength(1)
public class Strlen {
    private final KevaDatabase database;

    @Autowired
    public Strlen(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return new IntegerReply(0);
        }
        int result = new String(value, StandardCharsets.UTF_8).length();
        return new IntegerReply(result);
    }
}
