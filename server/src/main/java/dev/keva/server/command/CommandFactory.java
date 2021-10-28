package dev.keva.server.command;

import dev.keva.server.core.AppFactory;
import dev.keva.server.protocol.resp.reply.BulkReply;
import dev.keva.server.protocol.resp.reply.StatusReply;
import dev.keva.store.StorageService;
import lombok.val;

public class CommandFactory {
    private static final StorageService storageService = AppFactory.getStorageService();

    public static BulkReply get(byte[] key) {
        val got = storageService.get(key);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }

    public static StatusReply set(byte[] key, byte[] val) {
        storageService.put(key, val);
        return new StatusReply("OK");
    }
}
