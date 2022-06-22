package dev.keva.core.command.impl.set;

import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import org.apache.commons.lang3.SerializationUtils;

import java.util.HashSet;

public abstract class SetBase {
    protected final KevaDatabase database;

    public SetBase(KevaDatabase database) {
        this.database = database;
    }

    protected HashSet<BytesKey> getSet(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        return (HashSet<BytesKey>) SerializationUtils.deserialize(value);
    }

    protected int add(byte[] key, byte[]... values) {
        byte[] value = database.get(key);
        HashSet<BytesKey> set;
        set = value == null ? new HashSet<>() : this.getSet(key);
        int count = 0;
        for (byte[] v : values) {
            boolean isNewElement = set.add(new BytesKey(v));
            if (isNewElement) {
                count++;
            }
        }
        database.put(key, SerializationUtils.serialize(set));
        return count;
    }

    protected int size(byte[] key) {
        HashSet<BytesKey> set = this.getSet(key);
        return set.size();
    }

    protected byte[][] diff(byte[]... keys) {
        HashSet<BytesKey> set = new HashSet<>();
        for (byte[] key : keys) {
            byte[] value = database.get(key);
            if (set.isEmpty() && value != null) {
                set.addAll((HashSet<BytesKey>) SerializationUtils.deserialize(value));
            } else if (value != null) {
                HashSet<BytesKey> set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
                set.removeAll(set1);
            }
        }
        byte[][] result = new byte[set.size()][];
        int i = 0;
        for (BytesKey v : set) {
            result[i++] = v.getBytes();
        }
        return result;
    }

    protected byte[][] inter(byte[]... keys) {
        HashSet<BytesKey> set = new HashSet<>();
        for (byte[] key : keys) {
            byte[] value = database.get(key);
            if (set.isEmpty() && value != null) {
                set.addAll((HashSet<BytesKey>) SerializationUtils.deserialize(value));
            } else if (value != null) {
                HashSet<BytesKey> set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
                set.retainAll(set1);
            }
        }
        byte[][] result = new byte[set.size()][];
        int i = 0;
        for (BytesKey v : set) {
            result[i++] = v.getBytes();
        }
        return result;
    }

    protected byte[][] members(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
        byte[][] result = new byte[set.size()][];
        int i = 0;
        for (BytesKey v : set) {
            result[i++] = v.getBytes();
        }
        return result;
    }

    protected boolean isMember(byte[] key, byte[] value) {
        byte[] got = database.get(key);
        if (got == null) {
            return false;
        }
        HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(got);
        return set.contains(new BytesKey(value));
    }

    protected int move(byte[] source, byte[] destination, byte[] value) {
        byte[] sourceValue = database.get(source);
        if (sourceValue == null) {
            return 0;
        }
        HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(sourceValue);
        if (set.remove(new BytesKey(value))) {
            byte[] destinationValue = database.get(destination);
            HashSet<BytesKey> set1;
            if (destinationValue == null) {
                set1 = new HashSet<>();
            } else {
                set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(destinationValue);
            }
            boolean result = set1.add(new BytesKey(value));
            database.put(source, SerializationUtils.serialize(set));
            database.put(destination, SerializationUtils.serialize(set1));
            return result ? 1 : 0;
        }
        return 0;
    }

    protected int remove(byte[] key, byte[]... values) {
        byte[] value = database.get(key);
        if (value == null) {
            return 0;
        }
        HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
        int result = 0;
        for (byte[] v : values) {
            if (set.remove(new BytesKey(v))) {
                result++;
            }
        }
        if (set.isEmpty()) {
            database.remove(key);
        } else {
            database.put(key, SerializationUtils.serialize(set));
        }
        return result;
    }

    protected byte[][] union(byte[]... keys) {
        HashSet<BytesKey> set = new HashSet<>();
        for (byte[] key : keys) {
            byte[] value = database.get(key);
            if (value != null) {
                HashSet<BytesKey> set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
                set.addAll(set1);
            }
        }
        byte[][] result = new byte[set.size()][];
        int i = 0;
        for (BytesKey v : set) {
            result[i++] = v.getBytes();
        }
        return result;
    }
}
