package com.jinyframework.keva.server.command;

import java.util.List;

import static com.jinyframework.keva.server.storage.StorageFactory.hashStore;

public class Del implements CommandHandler {
    @Override
    public Object handle(List<String> args) {
        try {
            hashStore().remove(args.get(0));
            return 1;
        } catch (Exception ignore) {
            return 0;
        }
    }
}
