package dev.keva.store;

import dev.keva.store.impl.ChronicleMapDatabaseImpl;
import lombok.Setter;

@Setter
public final class DatabaseFactory {
    public synchronized static KevaDatabase createChronicleMapDatabase(DatabaseConfig config) {
        return new ChronicleMapDatabaseImpl(config);
    }
}
