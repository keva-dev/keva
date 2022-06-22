package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@CommandImpl("keys")
@ParamLength(1)
public class Key {
    private final KevaDatabase database;

    @Autowired
    public Key(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[] key) {
        String keyStr = new String(key, StandardCharsets.UTF_8);
        if (!keyStr.equals("*")) {
            throw new CommandException("Only support * pattern for now");
        }
        Set<byte[]> keys = database.keySet();
        BulkReply[] replies = new BulkReply[keys.size()];
        int i = 0;
        for (byte[] k : keys) {
            replies[i++] = new BulkReply(k);
        }
        return new MultiBulkReply(replies);
    }
}
