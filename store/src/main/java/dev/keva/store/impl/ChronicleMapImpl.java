package dev.keva.store.impl;

import dev.keva.store.DatabaseConfig;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ChronicleMapImpl implements KevaDatabase {
    private final SpinLock lock = new SpinLock();

    private ChronicleMap<byte[], byte[]> chronicleMap;

    public ChronicleMapImpl(DatabaseConfig config) {
        try {
            ChronicleMapBuilder<byte[], byte[]> mapBuilder = ChronicleMapBuilder.of(byte[].class, byte[].class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleSampleSampleKey".getBytes())
                    .averageValue("SampleSampleSampleSampleSampleSampleValue".getBytes())
                    .entries(100);

            boolean shouldPersist = config.getSnapshotEnabled();
            if (shouldPersist) {
                String snapshotDir = config.getSnapshotLocation();
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
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
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
