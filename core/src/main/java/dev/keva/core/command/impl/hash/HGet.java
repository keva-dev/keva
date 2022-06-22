package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("hget")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
public class HGet extends HashBase {
    private final KevaDatabase database;

    @Autowired
    public HGet(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] field) {
        byte[] got = this.get(key, field);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
