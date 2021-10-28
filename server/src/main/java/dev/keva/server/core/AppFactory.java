package dev.keva.server.core;

import dev.keva.server.config.ConfigHolder;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.DatabaseFactory;
import dev.keva.store.KevaDatabase;
import lombok.Setter;
import lombok.val;

public final class AppFactory {
    @Setter
    private static ConfigHolder config;
    @Setter
    private static KevaDatabase kevaDatabase;

    public static synchronized ConfigHolder getConfig() {
        if (config == null) {
            config = ConfigHolder.makeDefaultConfig();
        }
        return config;
    }

    public static synchronized void eagerInitKevaDatabase() {
        getKevaDatabase();
    }

    public static synchronized KevaDatabase getKevaDatabase() {
        if (kevaDatabase == null) {
            val dbConfig = DatabaseConfig.builder()
                    .heapSize(config.getHeapSize())
                    .snapshotEnabled(config.getSnapshotEnabled())
                    .snapshotLocation(config.getSnapshotLocation())
                    .build();
            kevaDatabase = DatabaseFactory.createChronicleMap(dbConfig);
        }
        return kevaDatabase;
    }
}
