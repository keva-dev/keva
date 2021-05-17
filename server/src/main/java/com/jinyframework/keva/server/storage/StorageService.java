package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;

public interface StorageService {
    // setter dep injection
    public void setStore(NoHeapStore store);

    public boolean putString(String key, String val);

    public String getString(String key);

    public boolean remove(String key);
}
