package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Set implements CommandHandler{
    @Override
    public Object handle(List<String> args) {
        if (args.size() < 2) {
            return 0;
        }
        return StorageService.getStringStringStore().put(args.get(0),args.get(1));
    }
}
