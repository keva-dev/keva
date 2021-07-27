package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NoHeapStorageServiceImpl implements StorageService {
    private static NoHeapStore store;

    @Override
    public void setStore(NoHeapStore store) {
        NoHeapStorageServiceImpl.store = store;
    }

    @Override
    public Path getSnapshotPath() {
        return Paths.get(store.getFolder());
    }

    @Override
    public boolean putString(String key, String val) {
        return store.putString(key,val);
    }

    @Override
    public String getString(String key) {
        return store.getString(key);
    }

    @Override
    public boolean remove(String key) {
        return store.remove(key);
    }
}
