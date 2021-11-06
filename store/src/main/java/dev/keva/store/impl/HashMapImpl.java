package dev.keva.store.impl;

import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;

import java.util.HashMap;
import java.util.Map;

public class HashMapImpl implements KevaDatabase {
    private final SpinLock lock = new SpinLock();

    private final Map<BytesKey, BytesValue> map = new HashMap<>(100);

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public void put(byte[] key, byte[] val) {
        lock.lock();
        try {
            map.put(new BytesKey(key), new BytesValue(val));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] get(byte[] key) {
        lock.lock();
        try {
            BytesValue got = map.get(new BytesKey(key));
            return got != null ? got.getBytes() : null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(byte[] key) {
        lock.lock();
        try {
            BytesValue removed = map.remove(new BytesKey(key));
            return removed != null;
        } finally {
            lock.unlock();
        }
    }
}
