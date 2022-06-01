package dev.keva.core.command.impl.list;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesValue;

import java.util.LinkedList;

@Component
@CommandImpl("lindex")
@ParamLength(2)
public class LIndex extends ListBase {
    private final KevaDatabase database;

    @Autowired
    public LIndex(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] indexByteArray) {
        LinkedList<BytesValue> list = this.getList(key);
        int index = Integer.parseInt(new String(indexByteArray));
        if (index < 0) {
            index = list.size() + index;
        }
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index) == null ? BulkReply.NIL_REPLY : new BulkReply(list.get(index).getBytes());
    }
}
