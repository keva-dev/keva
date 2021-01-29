package com.jinyframework.keva.server.command;

import java.util.List;

import static com.jinyframework.keva.server.storage.StorageFactory.hashStore;

public class Get implements CommandHandler {
    @Override
    public Object handle(List<String> args) {
        return hashStore().get(args.get(0));
    }
}
