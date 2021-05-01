package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;

// TODO: refactor to use 1 thread work queue
public class NoHeapStorageServiceImpl implements StorageService {
    private static NoHeapStore store;

    public void setStore(NoHeapStore store) {
        NoHeapStorageServiceImpl.store = store;
    }

    public boolean putString(String key, String val) {
        return store.putString(key, val);
    }

    public String getString(String key) {
        return store.getString(key);
    }

    public boolean remove(String key) {
        return store.remove(key);
    }
}
