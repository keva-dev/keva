package dev.keva.store;

import dev.keva.store.impl.OffHeapDatabaseImpl;
import dev.keva.store.impl.OnHeapDatabaseImpl;
import lombok.Setter;

@Setter
public final class DatabaseFactory {
    public synchronized static KevaDatabase createOffHeapDatabase(DatabaseConfig config) {
        return new OffHeapDatabaseImpl(config);
    }

    public synchronized static KevaDatabase createOnHeapDatabase() {
        return new OnHeapDatabaseImpl();
    }
}
