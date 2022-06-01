package dev.keva.core.command.impl.server;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("flushdb")
@ParamLength(type = ParamLength.Type.AT_MOST, value = 1)
public class FlushDB {
    private final KevaDatabase database;

    @Autowired
    public FlushDB(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[] ignored) {
        database.flush();
        return StatusReply.OK;
    }
}
