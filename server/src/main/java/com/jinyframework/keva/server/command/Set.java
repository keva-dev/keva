package com.jinyframework.keva.server.command;

import com.jinyframework.keva.store.NoHeapStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;

public class Set implements CommandHandler {
    private final NoHeapStore kevaStore = StorageFactory.getNoHeapDBStore();

    @Override
    public Object handle(List<String> args) {
        try {
            kevaStore.putString(args.get(0), args.get(1));
            return 1;
        } catch (Exception ignore) {
            return 0;
        }
    }
}
