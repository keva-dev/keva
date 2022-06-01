package dev.keva.core.command.impl.hash;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;

import java.util.HashMap;
import java.util.Map;

@Component
@CommandImpl("hkeys")
@ParamLength(1)
public class HKeys extends HashBase {
    private final KevaDatabase database;

    @Autowired
    public HKeys(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[] key) {
        HashMap<BytesKey, BytesValue> map = this.getMap(key);
        byte[][] got = new byte[map.size()][];
        int i = 0;
        for (Map.Entry<BytesKey, BytesValue> entry : map.entrySet()) {
            got[i++] = entry.getKey().getBytes();
        }
        BulkReply[] replies = new BulkReply[got.length];
        for (i = 0; i < got.length; i++) {
            replies[i] = new BulkReply(got[i]);
        }
        return new MultiBulkReply(replies);
    }
}
