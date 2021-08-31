package com.jinyframework.keva.store.legacy;

import java.util.HashMap;

public class LegacyNoHeapStoreManager {
    protected final static int MEGABYTE = 1024 * 1024;
    protected final static int DEFAULT_STORE_SIZE = MEGABYTE * 100;

    HashMap<String, LegacyNoHeapStore> stores = new HashMap<>();

    public LegacyNoHeapStoreManager() {
    }

    public boolean createStore(String name) {
        return createStore(name,
                LegacyNoHeapStore.Storage.IN_MEMORY,
                100);
    }

    public boolean createStore(String name,
                               LegacyNoHeapStore.Storage storageType) {
        return createStore(name,
                storageType,
                DEFAULT_STORE_SIZE);
    }

    public boolean createStore(String name,
                               LegacyNoHeapStore.Storage storageType,
                               int size) {
        return createStore(name, storageType, size, System.getProperty("user.dir"));
    }

    public boolean createStore(String name,
                               LegacyNoHeapStore.Storage storageType,
                               int size,
                               String homeDirectory) {
        LegacyNoHeapStoreImpl noHeapDB = new
                LegacyNoHeapStoreImpl(homeDirectory, name, storageType,
                size * MEGABYTE, true);

        stores.put(name, noHeapDB);

        return true;
    }

    public boolean createStore(String name,
                               LegacyNoHeapStore.Storage storageType,
                               int size,
                               String homeDirectory,
                               boolean reuseExisting) {
        LegacyNoHeapStoreImpl noHeapDB = new
                LegacyNoHeapStoreImpl(homeDirectory, name, storageType,
                                size * MEGABYTE, reuseExisting);

        stores.put(name, noHeapDB);

        return true;
    }

    public LegacyNoHeapStore getStore(String storeName) {
        return this.stores.get(storeName);
    }

    public boolean deleteStore(String storeName) {
        LegacyNoHeapStore store = this.stores.get(storeName);
        if (store != null) {
            // Delete the store here
            store.delete();
            return stores.remove(storeName) != null;
        }
        return false;
    }
}
