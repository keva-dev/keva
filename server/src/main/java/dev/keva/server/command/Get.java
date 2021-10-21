package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.BulkReply;
import dev.keva.server.storage.StorageService;

import java.util.List;

public class Get implements CommandHandler {
    private final StorageService storageService;

    public Get(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public BulkReply handle(List<String> args) {
        String got = storageService.getString(args.get(1));
        return got != null ? new BulkReply(got) : BulkReply.NIL_REPLY;
    }
}
