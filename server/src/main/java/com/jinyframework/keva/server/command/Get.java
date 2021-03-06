package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.noheap.NoHeapStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;

public class Get implements CommandHandler {
    private final NoHeapStore kevaStore = StorageFactory.getNoHeapDBStore();

    @Override
    public Object handle(List<String> args) {
        return kevaStore.getString(args.get(0));
    }
}
