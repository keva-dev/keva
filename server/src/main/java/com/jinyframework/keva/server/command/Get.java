package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Get implements CommandHandler {
    private final StorageService storageService;

    public Get(StorageService storageService) {
        this.storageService = storageService;
    }


    @Override
    public String handle(List<String> args) {
        return storageService.getString(args.get(0));
    }
}
