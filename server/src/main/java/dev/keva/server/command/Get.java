package dev.keva.server.command;

import dev.keva.server.command.annotation.Command;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.core.AppFactory;
import dev.keva.server.protocol.resp.reply.BulkReply;
import dev.keva.server.protocol.resp.reply.Reply;
import dev.keva.store.StorageService;
import lombok.val;

@Command("get")
public class Get {
    private static final StorageService storageService = AppFactory.getStorageService();

    @Execute
    public static Reply<?> execute(byte[] key) {
        val got = storageService.get(key);
        return got == null ? BulkReply.NIL_REPLY : new BulkReply(got);
    }
}
