package com.jinyframework.keva.server.noheap;

import com.jinyframework.keva.server.config.ConfigManager;

import java.util.HashMap;

public class NoHeapStoreManager {
    protected final static int MEGABYTE = 1024 * 1024;
    protected final static int DEFAULT_STORE_SIZE = MEGABYTE * 100;

    HashMap<String, NoHeapStore> stores = new HashMap<>();

    String snapshotConnection = ConfigManager.getConfig().getSnapshotLocation();
    String homeDirectory = snapshotConnection.length() == 0 ? System.getProperty("user.dir") : snapshotConnection;

    public NoHeapStoreManager() {
    }

    public NoHeapStoreManager(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public boolean createStore(String name) throws Exception {
        return createStore(name,
                NoHeapStore.Storage.IN_MEMORY,
                100);
    }

    public boolean createStore(String name,
                               NoHeapStore.Storage storageType) {
        return createStore(name,
                storageType,
                DEFAULT_STORE_SIZE);
    }

    public boolean createStore(String name,
                               NoHeapStore.Storage storageType,
                               int size) {
        NoHeapStoreImpl noHeapDB = new
                NoHeapStoreImpl(homeDirectory, name, storageType,
                size * MEGABYTE, true);

        stores.put(name, noHeapDB);

        return true;
    }

    public NoHeapStore getStore(String storeName) {
        return this.stores.get(storeName);
    }

    public boolean deleteStore(String storeName) {
        NoHeapStore store = this.stores.get(storeName);
        if (store != null) {
            // Delete the store here
            store.delete();
            return stores.remove(storeName) != null;
        }
        return false;
    }
}
