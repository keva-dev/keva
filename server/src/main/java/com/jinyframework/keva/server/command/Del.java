package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.command.setup.CommandHandler;
import com.jinyframework.keva.server.protocol.redis.IntegerReply;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Del implements CommandHandler {
    private final StorageService storageService;

    public Del(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public IntegerReply handle(List<String> args) {
        return storageService.remove(args.get(1))
                ? new IntegerReply(1)
                : new IntegerReply(0);
    }
}
