package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("incrbyfloat")
@ParamLength(type = EXACT, value = 2)
@Mutate
public class IncrByFloat {
    private final KevaDatabase database;

    @Autowired
    public IncrByFloat(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] incr) {
        double amount = Double.parseDouble(new String(incr, StandardCharsets.UTF_8));
        double curVal = 0L;
        try {
            byte[] oldVal = database.get(key);
            if (oldVal != null) {
                curVal = Double.parseDouble(new String(oldVal, StandardCharsets.UTF_8));
            }
            curVal = curVal + amount;
            database.put(key, Double.toString(curVal).getBytes());
        } catch (NumberFormatException ex) {
            throw new CommandException("Failed to parse integer from value stored");
        }
        return new BulkReply(Double.toString(curVal).getBytes());
    }
}
