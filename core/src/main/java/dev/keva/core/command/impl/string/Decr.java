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
        byte[] newVal;
        try {
            newVal = database.decrby(key, 1L);
        } catch (NumberFormatException ex) {
            throw new CommandException("value is not an integer or out of range");
        }
        return new IntegerReply(Long.parseLong(new String(newVal, StandardCharsets.UTF_8)));
    }
}
