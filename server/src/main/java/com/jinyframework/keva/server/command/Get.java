package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Get implements CommandHandler {
    private final StorageService storageService = ServiceInstance.getStorageService();

    @Override
    public Object handle(List<String> args) {
        return storageService.getString(args.get(0));
    }
}
