package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.KevaStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;

public class Del implements CommandHandler {
    private final KevaStore kevaStore = StorageFactory.getKevaStore();

    @Override
    public Object handle(List<String> args) {
        try {
            kevaStore.remove(args.get(0));
            return 1;
        } catch (Exception ignore) {
            return 0;
        }
    }
}
