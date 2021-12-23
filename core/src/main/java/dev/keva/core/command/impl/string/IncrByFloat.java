package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.store.KevaDatabase;

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
        byte[] newVal;
        try {
            double amount = Double.parseDouble(new String(incr, StandardCharsets.UTF_8));
            newVal = database.incrbyfloat(key, amount);
        } catch (NumberFormatException ex) {
            throw new CommandException("Value is not a valid float");
        }
        return new BulkReply(newVal);
    }
}
