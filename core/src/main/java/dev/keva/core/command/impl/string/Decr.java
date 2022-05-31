package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;


@Component
@CommandImpl("decr")
@ParamLength(type = EXACT, value = 1)
public class Decr {
    private final KevaDatabase database;

    @Autowired
    public Decr(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        long curVal = 0;
        try {
            byte[] oldVal = database.get(key);
            if (oldVal != null) {
                curVal = Long.parseLong(new String(oldVal, StandardCharsets.UTF_8));
            }
            curVal = curVal - 1;
            database.put(key, Long.toString(curVal).getBytes());
        } catch (NumberFormatException ex) {
            throw new CommandException("Failed to parse integer from value stored");
        }
        return new IntegerReply(curVal);
    }
}
