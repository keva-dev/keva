package dev.keva.store.impl;

import dev.keva.store.type.ZSet;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;
import dev.keva.store.KevaDatabase;
import dev.keva.store.lock.SpinLock;
import lombok.Getter;
import lombok.val;
import lombok.var;
import org.apache.commons.lang3.SerializationUtils;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static dev.keva.util.Constants.*;
import static dev.keva.util.Constants.FLAG_GT;

public class OnHeapDatabaseImpl implements KevaDatabase {
    @Getter
    private final SpinLock lock = new SpinLock();

    private final Map<BytesKey, BytesValue> map = new HashMap<>(100);

    @Override
    public void flush() {
        map.clear();
    }

    @Override
    public void put(byte[] key, byte[] val) {
        lock.exclusiveLock();
        try {
            map.put(new BytesKey(key), new BytesValue(val));
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public byte[] get(byte[] key) {
        lock.sharedLock();
        try {
            BytesValue got = map.get(new BytesKey(key));
            return got != null ? got.getBytes() : null;
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    public boolean remove(byte[] key) {
        lock.exclusiveLock();
        try {
            BytesValue removed = map.remove(new BytesKey(key));
            return removed != null;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public byte[] incrBy(byte[] key, long amount) {
        lock.exclusiveLock();
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
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] hget(byte[] key, byte[] field) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            HashMap<BytesKey, BytesValue> map = (HashMap<BytesKey, BytesValue>) SerializationUtils.deserialize(value);
            BytesValue got = map.get(new BytesKey(field));
            return got != null ? got.getBytes() : null;
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hgetAll(byte[] key) {
        lock.sharedLock();
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
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hkeys(byte[] key) {
        lock.sharedLock();
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
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] hvals(byte[] key) {
        lock.sharedLock();
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
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void hset(byte[] key, byte[] field, byte[] value) {
        lock.exclusiveLock();
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
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hdel(byte[] key, byte[] field) {
        lock.exclusiveLock();
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
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int lpush(byte[] key, byte[]... values) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            for (byte[] v : values) {
                list.addFirst(new BytesValue(v));
            }
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
            return list.size();
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int rpush(byte[] key, byte[]... values) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            for (byte[] v : values) {
                list.addLast(new BytesValue(v));
            }
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
            return list.size();
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] lpop(byte[] key) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (list.isEmpty()) {
                return null;
            }
            BytesValue v = list.removeFirst();
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
            return v.getBytes();
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] rpop(byte[] key) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (list.isEmpty()) {
                return null;
            }
            BytesValue v = list.removeLast();
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
            return v.getBytes();
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int llen(byte[] key) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            return list.size();
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] lrange(byte[] key, int start, int end) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            int size = list.size();
            if (start < 0) {
                start = size + start;
            }
            if (end < 0) {
                end = size + end;
            }
            if (start < 0) {
                start = 0;
            }
            if (end > size) {
                end = size;
            }
            if (start > end) {
                return null;
            }
            List<byte[]> result = new ArrayList<>(0);
            for (int j = start; j <= end; j++) {
                try {
                    if (list.get(j) != null) {
                        result.add(list.get(j).getBytes());
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
            return result.toArray(new byte[0][0]);
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] lindex(byte[] key, int index) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (index < 0) {
                index = list.size() + index;
            }
            if (index < 0 || index >= list.size()) {
                return null;
            }
            return list.get(index).getBytes();
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void lset(byte[] key, int index, byte[] value) {
        lock.exclusiveLock();
        try {
            byte[] v = map.get(new BytesKey(key)).getBytes();
            LinkedList<BytesValue> list;
            list = v == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(v);
            if (index < 0) {
                index = list.size() + index;
            }
            if (index < 0 || index >= list.size()) {
                return;
            }
            list.set(index, new BytesValue(value));
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int lrem(byte[] key, int count, byte[] value) {
        lock.exclusiveLock();
        try {
            byte[] value1 = map.get(new BytesKey(key)).getBytes();
            if (value1 == null) {
                return 0;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value1);
            int size = list.size();
            int result = 0;
            if (count > 0) {
                for (int i = 0; i < size; i++) {
                    if (Arrays.equals(list.get(i).getBytes(), value)) {
                        if (count != 0) {
                            count--;
                            list.remove(i);
                            result++;
                            size--;
                        }
                    }
                }
            } else if (count < 0) {
                for (int i = size - 1; i >= 0; i--) {
                    if (Arrays.equals(list.get(i).getBytes(), value)) {
                        if (count != 0) {
                            count++;
                            list.remove(i);
                            result++;
                            size--;
                        }
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    if (Arrays.equals(list.get(i).getBytes(), value)) {
                        list.remove(i);
                        result++;
                        size--;
                    }
                }
            }
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(list)));
            return result;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int sadd(byte[] key, byte[]... values) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            HashSet<BytesKey> set;
            set = value == null ? new HashSet<>() : (HashSet<BytesKey>) SerializationUtils.deserialize(value);
            int count = 0;
            for (byte[] v : values) {
                boolean isNewElement = set.add(new BytesKey(v));
                if (isNewElement) {
                    count++;
                }
            }
            map.put(new BytesKey(key), new BytesValue(SerializationUtils.serialize(set)));
            return count;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] smembers(byte[] key) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
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
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean sismember(byte[] key, byte[] value) {
        lock.sharedLock();
        try {
            byte[] got = map.get(new BytesKey(key)).getBytes();
            if (got == null) {
                return false;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(got);
            return set.contains(new BytesKey(value));
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int scard(byte[] key) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return 0;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
            return set.size();
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sdiff(byte[]... keys) {
        lock.sharedLock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = map.get(new BytesKey(key)).getBytes();
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
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sinter(byte[]... keys) {
        lock.sharedLock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = map.get(new BytesKey(key)).getBytes();
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
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sunion(byte[]... keys) {
        lock.sharedLock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = map.get(new BytesKey(key)).getBytes();
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
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int smove(byte[] source, byte[] destination, byte[] value) {
        lock.exclusiveLock();
        try {
            byte[] sourceValue = map.get(new BytesKey(source)).getBytes();
            if (sourceValue == null) {
                return 0;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(sourceValue);
            if (set.remove(new BytesKey(value))) {
                byte[] destinationValue = map.get(new BytesKey(destination)).getBytes();
                HashSet<BytesKey> set1;
                if (destinationValue == null) {
                    set1 = new HashSet<>();
                } else {
                    set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(destinationValue);
                }
                set1.add(new BytesKey(value));
                map.put(new BytesKey(destination), new BytesKey(SerializationUtils.serialize(set1)));
                map.put(new BytesKey(source), new BytesKey(SerializationUtils.serialize(set)));
                return 1;
            }
            return 0;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int srem(byte[] key, byte[]... values) {
        lock.exclusiveLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return 0;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
            int count = 0;
            for (byte[] v : values) {
                if (set.remove(new BytesKey(v))) {
                    count++;
                }
            }
            if (set.isEmpty()) {
                map.remove(new BytesKey(key));
            } else {
                map.put(new BytesKey(key), new BytesKey(SerializationUtils.serialize(set)));
            }
            return count;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public int strlen(byte[] key) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return 0;
            }
            return new String(value, StandardCharsets.UTF_8).length();
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    public int setrange(byte[] key, byte[] offset, byte[] val) {
        lock.exclusiveLock();
        try {
            var offsetPosition = Integer.parseInt(new String(offset, StandardCharsets.UTF_8));
            byte[] oldVal = map.get(new BytesKey(key)).getBytes();
            int newValLength = oldVal == null ? offsetPosition + val.length : Math.max(offsetPosition + val.length, oldVal.length);
            byte[] newVal = new byte[newValLength];
            for (int i = 0; i < newValLength; i++) {
                if (i >= offsetPosition && i < offsetPosition + val.length) {
                    newVal[i] = val[i - offsetPosition];
                } else if (oldVal != null && i < oldVal.length) {
                    newVal[i] = oldVal[i];
                } else {
                    newVal[i] = 0b0;
                }
            }
            map.put(new BytesKey(key), new BytesValue(newVal));
            return newValLength;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public byte[][] mget(byte[]... keys) {
        lock.sharedLock();
        try {
            byte[][] result = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                byte[] key = keys[i];
                val got = map.get(new BytesKey(key)).getBytes();
                result[i] = got;
            }
            return result;
        } finally {
            lock.sharedUnlock();
        }
    }

    @Override
    public int zadd(byte[] key, AbstractMap.SimpleEntry<Double, BytesKey>[] members, int flags) {
        boolean xx = (flags & FLAG_XX) != 0;
        boolean nx = (flags & FLAG_NX) != 0;
        boolean lt = (flags & FLAG_LT) != 0;
        boolean gt = (flags & FLAG_GT) != 0;
        boolean ch = (flags & FLAG_CH) != 0;

        // Track both to eliminate conditional branch
        int added = 0, changed = 0;

        lock.exclusiveLock();
        try {
            final BytesKey mapKey = new BytesKey(key);
            byte[] value = map.get(mapKey).getBytes();
            ZSet zSet;
            zSet = value == null ? new ZSet() : (ZSet) SerializationUtils.deserialize(value);
            for (AbstractMap.SimpleEntry<Double, BytesKey> member : members) {
                Double currScore = zSet.getScore(member.getValue());
                if (currScore == null) {
                    if (xx) {
                        continue;
                    }
                    currScore = member.getKey();
                    zSet.add(new AbstractMap.SimpleEntry<>(currScore, member.getValue()));
                    ++added;
                    ++changed;
                    continue;
                }
                Double newScore = member.getKey();
                if(nx || (lt && newScore >= currScore) || (gt && newScore <= currScore)) {
                    continue;
                }
                if (!newScore.equals(currScore)) {
                    zSet.removeByKey(member.getValue());
                    zSet.add(member);
                    ++changed;
                }
            }
            map.put(mapKey, new BytesValue(SerializationUtils.serialize(zSet)));
            return ch ? changed : added;
        } finally {
            lock.exclusiveUnlock();
        }
    }

    @Override
    public Double zincrby(byte[] key, Double incr, BytesKey e, int flags) {
        lock.exclusiveLock();
        try {
            final BytesKey mapKey = new BytesKey(key);
            byte[] value = map.get(mapKey).getBytes();
            ZSet zSet;
            zSet = value == null ? new ZSet() : (ZSet) SerializationUtils.deserialize(value);
            Double currentScore = zSet.getScore(e);
            if (currentScore == null) {
                if ((flags & FLAG_XX) != 0) {
                    return null;
                }
                currentScore = incr;
                zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
                map.put(mapKey, new BytesValue(SerializationUtils.serialize(zSet)));
                return currentScore;
            }
            if ((flags & FLAG_NX) != 0) {
                return null;
            }
            if ((flags & FLAG_LT) != 0 && incr >= 0) {
                return null;
            }
            if ((flags & FLAG_GT) != 0 && incr <= 0) {
                return null;
            }
            zSet.remove(new AbstractMap.SimpleEntry<>(currentScore, e));
            currentScore += incr;
            zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
            map.put(mapKey, new BytesValue(SerializationUtils.serialize(zSet)));
            return currentScore;
        } finally {
            lock.exclusiveUnlock();
        }

    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        lock.sharedLock();
        try {
            byte[] value = map.get(new BytesKey(key)).getBytes();
            if (value == null) {
                return null;
            }
            ZSet zset = (ZSet) SerializationUtils.deserialize(value);
            return zset.getScore(new BytesKey(member));
        } finally {
            lock.sharedUnlock();
        }
    }
}
