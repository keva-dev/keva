package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("getset")
@ParamLength(type = EXACT, value = 2)
@Mutate
public class GetSet {
    private final KevaDatabase database;

    @Autowired
    public GetSet(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] val) {
        byte[] got = database.get(key);
        database.put(key, val);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
