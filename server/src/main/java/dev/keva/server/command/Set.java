package dev.keva.server.command;

import dev.keva.server.command.annotation.Command;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.core.AppFactory;
import dev.keva.server.protocol.resp.reply.StatusReply;
import dev.keva.store.StorageService;

@Command("set")
public class Set {
    private static final StorageService storageService = AppFactory.getStorageService();

    @Execute
    public static StatusReply set(byte[] key, byte[] val) {
        storageService.put(key, val);
        return new StatusReply("OK");
    }
}
