package dev.keva.store;

import dev.keva.store.impl.ChronicleMapImpl;
import lombok.Setter;

@Setter
public final class DatabaseFactory {
    public synchronized static KevaDatabase createChronicleMap(DatabaseConfig config) {
        return new ChronicleMapImpl(config);
    }
}
