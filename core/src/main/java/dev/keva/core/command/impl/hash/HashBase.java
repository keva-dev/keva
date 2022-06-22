package dev.keva.core.command.impl.hash;

import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;
import org.apache.commons.lang3.SerializationUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class HashBase {
    protected final KevaDatabase database;

    public HashBase(KevaDatabase database) {
        this.database = database;
    }

    protected byte[] get(byte[] key, byte[] field) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
        BytesValue got = map.get(new BytesKey(field));
        return got == null ? null : got.getBytes();
    }

    protected HashMap<BytesKey, BytesValue> getMap(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        return (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
    }

    protected byte[][] getAll(byte[] key) {
        byte[] value = database.get(key);
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
    }

    protected void set(byte[] key, byte[] field, byte[] value) {
        HashMap<BytesKey, BytesValue> map;
        byte[] oldValue = database.get(key);
        if (oldValue == null) {
            map = new HashMap<>();
        } else {
            map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(oldValue);
        }
        map.put(new BytesKey(field), new BytesValue(value));
        database.put(key, SerializationUtils.serialize(map));
    }

    protected boolean delete(byte[] key, byte[] field) {
        byte[] value = database.get(key);
        if (value == null) {
            return false;
        }
        HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
        boolean result = map.remove(new BytesKey(field)) != null;
        if (result) {
            database.put(key, SerializationUtils.serialize(map));
        }
        return result;
    }
}
