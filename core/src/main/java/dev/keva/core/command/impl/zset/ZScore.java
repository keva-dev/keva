package dev.keva.core.command.impl.zset;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

@Component
@CommandImpl("zscore")
@ParamLength(type = ParamLength.Type.EXACT, value = 2)
public final class ZScore extends ZBase {
    private final KevaDatabase database;

    @Autowired
    public ZScore(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] member) {
        Double result = this.score(key, member);
        if (result == null) {
            return BulkReply.NIL_REPLY;
        }
        if (result.equals(Double.POSITIVE_INFINITY)) {
            return BulkReply.POSITIVE_INFINITY_REPLY;
        }
        if (result.equals(Double.NEGATIVE_INFINITY)) {
            return BulkReply.NEGATIVE_INFINITY_REPLY;
        }
        return new BulkReply(result.toString());
    }
}
