package dev.keva.store.impl;

import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class HashMapImpl implements KevaDatabase {
    @Getter
    private final Lock lock = new SpinLock();

    private final Map<BytesKey, BytesValue> map = new HashMap<>(100);

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

    @Override
    public byte[] incrBy(byte[] key, long amount) {
        lock.lock();
        try {
            return map.compute(new BytesKey(key), (k, oldVal) -> {
                long curVal = 0L;
                if (oldVal != null) {
                    curVal = Long.parseLong(oldVal.toString());
                }
                curVal = curVal + amount;
                return new BytesValue(Long.toString(curVal).getBytes(StandardCharsets.UTF_8));
            }).getBytes();
        } finally {
            lock.unlock();
        }
    }
}
