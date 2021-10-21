package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;

import java.nio.file.Path;

public interface StorageService {
    Path getSnapshotPath();

    boolean putString(String key, String val);

    String getString(String key);

    boolean remove(String key);
}
