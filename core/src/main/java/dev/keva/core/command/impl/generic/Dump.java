package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;

import java.util.Base64;

@Component
@CommandImpl("dump")
@ParamLength(1)
public class Dump {
    private final KevaDatabase database;

    @Autowired
    public Dump(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[] key) {
        byte[] got = database.get(key);
        if (got == null) {
            return BulkReply.NIL_REPLY;
        }
        String dumped = Base64.getEncoder().encodeToString(got);
        return new BulkReply(dumped);
    }
}
