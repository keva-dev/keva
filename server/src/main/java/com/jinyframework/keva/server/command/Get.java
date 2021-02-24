package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.KevaStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;

public class Get implements CommandHandler {
    private final KevaStore kevaStore = StorageFactory.getKevaStore();

    @Override
    public Object handle(List<String> args) {
        return kevaStore.get(args.get(0));
    }
}
