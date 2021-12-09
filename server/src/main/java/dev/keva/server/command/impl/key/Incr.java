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
@CommandImpl("incr")
@ParamLength(type = EXACT, value = 1)
@Mutate
public class Incr {
    private final KevaDatabase database;

    @Autowired
    public Incr(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        byte[] newVal;
        try {
            newVal = database.incrBy(key, 1L);
        } catch (NumberFormatException ex) {
            throw new CommandException("Failed to parse integer from value stored");
        }
        return new IntegerReply(Long.parseLong(new String(newVal, StandardCharsets.UTF_8)));
    }
}
