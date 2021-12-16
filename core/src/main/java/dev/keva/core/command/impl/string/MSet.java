package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("mset")
@ParamLength(type = AT_LEAST, value = 2)
public class MSet {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public MSet(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public StatusReply execute(byte[]... keys) {
        if (keys.length % 2 != 0) {
            throw new CommandException("Wrong number of arguments for MSET");
        }
        database.mset(keys);
        return StatusReply.OK;
    }
}
