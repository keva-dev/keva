package dev.keva.server.core;

import dev.keva.server.config.ConfigHolder;
import dev.keva.store.NoHeapConfig;
import dev.keva.store.NoHeapFactory;
import dev.keva.store.StorageService;
import lombok.Setter;
import lombok.val;

public final class AppFactory {
    @Setter
    private static ConfigHolder config;
    @Setter
    private static StorageService storageService;

    public static synchronized ConfigHolder getConfig() {
        if (config == null) {
            config = ConfigHolder.makeDefaultConfig();
        }
        return config;
    }

    public static synchronized void eagerInitStorageService() {
        getStorageService();
    }

    public static synchronized StorageService getStorageService() {
        if (storageService == null) {
            val noHeapConfig = NoHeapConfig.builder()
                    .heapSize(config.getHeapSize())
                    .snapshotEnabled(config.getSnapshotEnabled())
                    .snapshotLocation(config.getSnapshotLocation())
                    .build();
            storageService = NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
        }
        return storageService;
    }
}
