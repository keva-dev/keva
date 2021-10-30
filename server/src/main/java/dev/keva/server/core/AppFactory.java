package dev.keva.server.core;

import dev.keva.server.config.KevaConfig;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.DatabaseFactory;
import dev.keva.store.KevaDatabase;
import lombok.Setter;
import lombok.val;

public final class AppFactory {
    @Setter
    private static KevaConfig config;
    @Setter
    private static KevaDatabase kevaDatabase;

    public static synchronized KevaConfig getConfig() {
        if (config == null) {
            config = KevaConfig.ofDefaults();
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
                    .snapshotEnabled(config.getPersistence())
                    .snapshotLocation(config.getWorkDirectory())
                    .build();
            kevaDatabase = DatabaseFactory.createChronicleMap(dbConfig);
        }
        return kevaDatabase;
    }
}
