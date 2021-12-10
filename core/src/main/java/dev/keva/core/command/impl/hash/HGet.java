package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("hget")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
public class HGet {
    private final KevaDatabase database;

    @Autowired
    public HGet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] field) {
        val got = database.hget(key, field);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
