package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("setex")
@ParamLength(type = ParamLength.Type.EXACT, value = 3)
public class SetEX {
    private final KevaDatabase database;

    @Autowired
    public SetEX(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[][] params) {
        long seconds;
        try {
            seconds = Long.parseLong(new String(params[1]));
        } catch (NumberFormatException e) {
            throw new CommandException("value is not an integer or out of range");
        }
        if (seconds < 1) {
            throw new CommandException("invalid expire time in 'setex' command");
        }
        database.put(params[0], params[2]);
        database.setExpiration(params[0], System.currentTimeMillis() + seconds * 1000);
        return StatusReply.OK;
    }
}
