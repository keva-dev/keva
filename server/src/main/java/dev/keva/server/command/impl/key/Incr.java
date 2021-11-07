package dev.keva.server.command.impl.key;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.exception.CommandException;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.server.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("incr")
@ParamLength(type = EXACT, value = 1)
public class Incr {
    private final KevaDatabase database;

    @Autowired
    public Incr(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key) {
        var afterIncr = 0L;
        database.getLock().lock();
        try {
            byte[] valBytes = database.get(key);
            long curVal = 0L;
            if (valBytes != null) {
                curVal = Long.parseLong(new String(valBytes, StandardCharsets.UTF_8));
            }
            curVal++;
            database.put(key, Long.toString(curVal).getBytes(StandardCharsets.UTF_8));
            afterIncr = curVal;
        } catch (NumberFormatException ex) {
            throw new CommandException("Failed to parse integer value from key");
        } finally {
            database.getLock().unlock();
        }
        return new IntegerReply(afterIncr);
    }
}
