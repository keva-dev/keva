package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("mset")
@ParamLength(type = AT_LEAST, value = 2)
public class MSet {
    private final KevaDatabase database;

    @Autowired
    public MSet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[]... keys) {
        if (keys.length % 2 != 0) {
            throw new CommandException("Wrong number of arguments for MSET");
        }
        for (int i = 0; i < keys.length; i += 2) {
            database.put(keys[i], keys[i + 1]);
        }
        return StatusReply.OK;
    }
}
