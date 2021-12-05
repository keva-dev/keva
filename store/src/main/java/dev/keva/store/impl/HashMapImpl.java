package dev.keva.store.impl;

import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.Getter;
import org.apache.commons.lang3.SerializationUtils;

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

    @Override
    @SuppressWarnings("unchecked")
    public byte[] hget(byte[] key, byte[] field) {
        lock.lock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            BytesValue got = map.get(new BytesKey(field));
            return got != null ? got.getBytes() : null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hgetAll(byte[] key) {
        lock.lock();
        try {
            BytesValue value = map.get(new BytesKey(key));
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value.getBytes());
            byte[][] result = new byte[map.size() * 2][];
            int i = 0;
            for (Map.Entry<BytesKey, BytesValue> entry : map.entrySet()) {
                result[i++] = entry.getKey().getBytes();
                result[i++] = entry.getValue().getBytes();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hkeys(byte[] key) {
        lock.lock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            byte[][] result = new byte[map.size()][];
            int i = 0;
            for (Map.Entry<BytesKey, BytesValue> entry : map.entrySet()) {
                result[i++] = entry.getKey().getBytes();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hvals(byte[] key) {
        lock.lock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            byte[][] result = new byte[map.size()][];
            int i = 0;
            for (Map.Entry<BytesKey, BytesValue> entry : map.entrySet()) {
                result[i++] = entry.getValue().getBytes();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void hset(byte[] key, byte[] field, byte[] value) {
        lock.lock();
        try {
            map.compute(new BytesKey(key), (k, oldVal) -> {
                HashMap<BytesKey, BytesValue> map;
                if (oldVal == null) {
                    map = new HashMap<>();
                } else {
                    map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(oldVal.getBytes());
                }
                map.put(new BytesKey(field), new BytesValue(value));
                return new BytesValue(SerializationUtils.serialize(map));
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hdel(byte[] key, byte[] field) {
        lock.lock();
        try {
            BytesValue value = map.get(new BytesKey(key));
            if (value == null) {
                return false;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value.getBytes());
            boolean removed = map.remove(new BytesKey(field)) != null;
            if (removed) {
                map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(map)));
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }
}
