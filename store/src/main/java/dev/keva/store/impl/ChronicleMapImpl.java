package dev.keva.store.impl;

import dev.keva.store.DatabaseConfig;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

@Slf4j
public class ChronicleMapImpl implements KevaDatabase {
    @Getter
    private final Lock lock = new SpinLock();

    private ChronicleMap<byte[], byte[]> chronicleMap;

    public ChronicleMapImpl(DatabaseConfig config) {
        try {
            ChronicleMapBuilder<byte[], byte[]> mapBuilder = ChronicleMapBuilder.of(byte[].class, byte[].class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleSampleSampleKey".getBytes())
                    .averageValue("SampleSampleSampleSampleSampleSampleValue".getBytes())
                    .entries(100);

            boolean shouldPersist = config.getIsPersistence();
            if (shouldPersist) {
                String snapshotDir = config.getWorkingDirectory();
                String location = snapshotDir.equals("./") ? "" : snapshotDir + "/";
                File file = new File(location + "dump.kdb");
                this.chronicleMap = mapBuilder.createPersistedTo(file);
            } else {
                this.chronicleMap = mapBuilder.create();
            }
        } catch (IOException e) {
            log.error("Failed to create ChronicleMap: ", e);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        lock.lock();
        try {
            return chronicleMap.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(byte[] key, byte[] val) {
        lock.lock();
        try {
            chronicleMap.put(key, val);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(byte[] key) {
        lock.lock();
        try {
            return chronicleMap.remove(key) != null;
        } finally {
            lock.unlock();
        }
    }
}
