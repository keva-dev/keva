package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.command.setup.CommandHandler;
import com.jinyframework.keva.server.protocol.redis.StatusReply;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Set implements CommandHandler {
    private final StorageService storageService;

    public Set(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public StatusReply handle(List<String> args) {
        storageService.putString(args.get(1), args.get(2));
        return StatusReply.OK;
    }
}
