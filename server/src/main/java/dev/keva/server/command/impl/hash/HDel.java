package dev.keva.server.command.impl.hash;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Component
@CommandImpl("hdel")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
public class HDel {
    private final KevaDatabase database;

    @Autowired
    public HDel(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public IntegerReply execute(byte[] key, byte[]... fields) {
        int deleted = 0;
        for (val field : fields) {
            val result = database.hdel(key, field);
            if (result) {
                deleted++;
            }
        }
        return new IntegerReply(deleted);
    }
}
