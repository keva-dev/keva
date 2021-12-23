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
@CommandImpl("decrby")
@ParamLength(type = EXACT, value = 2)
public class Decrby {
    private final KevaDatabase database;

    @Autowired
    public Decrby(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] decrBy) {
        long amount = Long.parseLong(new String(decrBy, StandardCharsets.UTF_8));
        byte[] newVal;
        try {
            newVal = database.decrby(key, amount);
        } catch (NumberFormatException ex) {
            throw new CommandException("value is not an integer or out of range");
        }
        return new IntegerReply(Long.parseLong(new String(newVal, StandardCharsets.UTF_8)));
    }
}
