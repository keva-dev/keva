package dev.keva.server.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.Mutate;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.exception.CommandException;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.server.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("incrby")
@ParamLength(type = EXACT, value = 2)
@Mutate
public class Incrby {
    private final KevaDatabase database;

    @Autowired
    public Incrby(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[] incrBy) {
        var amount = Long.parseLong(new String(incrBy, StandardCharsets.UTF_8));
        byte[] newVal;
        try {
            newVal = database.incrBy(key, amount);
        } catch (NumberFormatException ex) {
            throw new CommandException("Failed to parse integer from value stored");
        }
        return new IntegerReply(Long.parseLong(new String(newVal, StandardCharsets.UTF_8)));
    }
}
