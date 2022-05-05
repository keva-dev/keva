package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("substr")
@ParamLength(3)
public class SubStr {
    private final KevaDatabase database;

    @Autowired
    public SubStr(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] start, byte[] end) {
        return new BulkReply(database.substr(key,
                Integer.parseInt(new String(start, StandardCharsets.UTF_8)),
                Integer.parseInt(new String(end, StandardCharsets.UTF_8))));
    }
}
