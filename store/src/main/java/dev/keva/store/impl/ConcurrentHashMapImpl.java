package dev.keva.store.impl;

import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.store.KevaDatabase;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashMapImpl implements KevaDatabase {
    private final ConcurrentMap<BytesKey, BytesValue> concurrentMap = new ConcurrentHashMap<>(1024);

    @Override
    public void shutdownGracefully() {
        concurrentMap.clear();
    }

    @Override
    public Path getSnapshotPath() {
        return null;
    }

    @Override
    public void put(byte[] key, byte[] val) {
        concurrentMap.put(new BytesKey(key), new BytesValue(val));
    }

    @Override
    public byte[] get(byte[] key) {
        BytesValue got = concurrentMap.get(new BytesKey(key));
        return got != null ? got.getBytes() : null;
    }

    @Override
    public boolean remove(byte[] key) {
        BytesValue removed = concurrentMap.remove(new BytesKey(key));
        return removed != null;
    }
}
