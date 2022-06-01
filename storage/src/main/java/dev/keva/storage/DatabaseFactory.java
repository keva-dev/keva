package dev.keva.storage;

import dev.keva.storage.impl.chroniclemap.ChronicleMapConfig;
import dev.keva.storage.impl.chroniclemap.ChronicleMapDatabaseImpl;
import lombok.Setter;

@Setter
public final class DatabaseFactory {
    public synchronized static KevaDatabase createChronicleMapDatabase(ChronicleMapConfig config) {
        return new ChronicleMapDatabaseImpl(config);
    }
}
