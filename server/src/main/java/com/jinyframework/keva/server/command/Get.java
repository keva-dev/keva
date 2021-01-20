package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Get implements CommandHandler{
    @Override
    public Object handle(List<String> args) {
        return StorageService.getStringStringStore().get(args.get(0));
    }
}
