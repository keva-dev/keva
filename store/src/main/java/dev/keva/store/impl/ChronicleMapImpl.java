package dev.keva.store.impl;

import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

    @Override
    public byte[] incrBy(byte[] key, long amount) {
        lock.lock();
        try {
            return chronicleMap.compute(key, (k, oldVal) -> {
                long curVal = 0L;
                if (oldVal != null) {
                    curVal = Long.parseLong(new String(oldVal, StandardCharsets.UTF_8));
                }
                curVal = curVal + amount;
                return Long.toString(curVal).getBytes(StandardCharsets.UTF_8);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] hget(byte[] key, byte[] field) {
        lock.lock();
        try {
            byte[] value = chronicleMap.get(key);
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            BytesValue got = map.get(new BytesKey(field));
            return got == null ? null : got.getBytes();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hgetAll(byte[] key) {
        lock.lock();
        try {
            byte[] value = chronicleMap.get(key);
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
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
            byte[] value = chronicleMap.get(key);
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
            byte[] value = chronicleMap.get(key);
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
            chronicleMap.compute(key, (k, oldVal) -> {
                HashMap<BytesKey, BytesValue> map;
                if (oldVal == null) {
                    map = new HashMap<>();
                } else {
                    map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(oldVal);
                }
                map.put(new BytesKey(field), new BytesValue(value));
                return SerializationUtils.serialize(map);
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
            byte[] value = chronicleMap.get(key);
            if (value == null) {
                return false;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            boolean result = map.remove(new BytesKey(field)) != null;
            if (result) {
                chronicleMap.put(key, SerializationUtils.serialize(map));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
