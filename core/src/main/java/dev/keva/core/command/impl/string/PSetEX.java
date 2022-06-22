package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("psetex")
@ParamLength(type = ParamLength.Type.EXACT, value = 3)
public class PSetEX {
    private final KevaDatabase database;

    @Autowired
    public PSetEX(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[][] params) {
        String key = new String(params[0], StandardCharsets.UTF_8);
        long milliseconds;
        try {
            milliseconds = Long.parseLong(new String(params[1]));
        } catch (NumberFormatException e) {
            throw new CommandException("value is not an integer or out of range");
        }
        if (milliseconds < 1) {
            throw new CommandException("invalid expire time in 'psetex' command");
        }
        database.put(params[0], params[2]);
        database.setExpiration(params[0], System.currentTimeMillis() + milliseconds);
        return StatusReply.OK;
    }
}
