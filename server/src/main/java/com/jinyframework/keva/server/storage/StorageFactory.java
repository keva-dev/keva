package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;
import com.jinyframework.keva.store.NoHeapStoreManager;
import com.jinyframework.keva.server.config.ConfigManager;
import com.jinyframework.keva.server.core.ServerSocket;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.ConcurrentHashMap;

@Setter
@Slf4j
public final class StorageFactory {
    private static NoHeapStore noHeapStore;
    private static ConcurrentHashMap<String, ServerSocket> socketHashMap;

    private StorageFactory() {
    }

    public static synchronized NoHeapStore getNoHeapDBStore() {
        if (noHeapStore == null) {
            try {
                val db = new NoHeapStoreManager();
                val shouldPersist = ConfigManager.getConfig().getSnapshotEnabled();
                val heapSizeInMegabytes = ConfigManager.getConfig().getHeapSize();
                db.createStore("Keva",
                        shouldPersist ? NoHeapStore.Storage.PERSISTED : NoHeapStore.Storage.IN_MEMORY,
                        heapSizeInMegabytes, ConfigManager.getConfig().getSnapshotLocation());
                noHeapStore = db.getStore("Keva");
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                log.error("Cannot get noHeapDbStore");
                System.exit(1);
            }
        }

        return noHeapStore;
    }

    public static synchronized ConcurrentHashMap<String, ServerSocket> getSocketHashMap() {
        if (socketHashMap == null) {
            socketHashMap = new ConcurrentHashMap<>();
        }

        return socketHashMap;
    }
}
