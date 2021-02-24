package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.KevaStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;

public class Expire implements CommandHandler {
    private final KevaStore kevaStore = StorageFactory.getKevaStore();

    @Override
    public Object handle(List<String> args) {
        try {
            kevaStore.expire(args.get(0), Long.parseLong(args.get(1)));
            return 1;
        } catch (Exception ignore) {
            return 0;
        }
    }
}
