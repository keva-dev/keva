package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.protocol.redis.BulkReply;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Get implements CommandHandler {
    private final StorageService storageService;

    public Get(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public BulkReply handle(List<String> args) {
        String got = storageService.getString(args.get(0));
        return got != null ? new BulkReply(got) : BulkReply.NIL_REPLY;
    }
}
