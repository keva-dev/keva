package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.key.manager.ExpirationManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.store.KevaDatabase;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("getrange")
@ParamLength(type = EXACT, value = 3)
public class GetRange {
    private final KevaDatabase database;
    private final ExpirationManager expirationManager;

    @Autowired
    public GetRange(KevaDatabase database, ExpirationManager expirationManager) {
        this.database = database;
        this.expirationManager = expirationManager;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] start, byte[] end) {
        return new BulkReply(database.getrange(key, start, end));
    }
}
