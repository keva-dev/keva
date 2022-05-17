package dev.keva.store.impl;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.keva.store.type.ZSet;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.KevaDatabase;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;

import static dev.keva.util.Constants.FLAG_CH;
import static dev.keva.util.Constants.FLAG_GT;
import static dev.keva.util.Constants.FLAG_LT;
import static dev.keva.util.Constants.FLAG_NX;
import static dev.keva.util.Constants.FLAG_XX;

@Slf4j
public class OffHeapDatabaseImpl implements KevaDatabase {
    private static final byte[] EXP_POSTFIX = new byte[]{(byte) 0x7f, (byte) 0x2f, (byte) 0x61, (byte) 0x74};
    @Getter
    private final Lock lock = new ReentrantLock();
    private ChronicleMap<byte[], byte[]> chronicleMap;

    public OffHeapDatabaseImpl(DatabaseConfig config) {
        try {
            ChronicleMapBuilder<byte[], byte[]> mapBuilder = ChronicleMapBuilder.of(byte[].class, byte[].class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleSampleSampleKey".getBytes())
                    .averageValue("SampleSampleSampleSampleSampleSampleValue".getBytes())
                    .entries(1_000_000);

            boolean shouldPersist = config.getIsPersistence();
            if (shouldPersist) {
                String workingDir = config.getWorkingDirectory();
                String location = workingDir.equals("./") ? "" : workingDir + "/";
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
    public void flush() {
        lock.lock();
        try {
            chronicleMap.clear();
        } finally {
            lock.unlock();
        }
    }

    private byte[] getExpireKey(byte[] key) {
        return Bytes.concat(key, EXP_POSTFIX);
    }

    private boolean isExpired(byte[] key) {
        byte[] longInBytes = chronicleMap.get(getExpireKey(key));
        if (longInBytes == null) {
            return false;
        } else {
            return Longs.fromByteArray(longInBytes) <= System.currentTimeMillis();
        }
    }

    @Override
    public void expireAt(byte[] key, long timestampInMillis) {
        byte[] expireKey = getExpireKey(key);
        byte[] timestampBytes = Longs.toByteArray(timestampInMillis);
        if (timestampInMillis <= System.currentTimeMillis()) {
            chronicleMap.remove(expireKey);
        } else {
            chronicleMap.put(expireKey, timestampBytes);
        }
    }

    @Override
    public boolean rename(byte[] key, byte[] newKey) {
        lock.lock();
        try {
            byte[] moveValue = chronicleMap.get(key);
            if (moveValue == null) {
                return false;
            }
            chronicleMap.put(newKey, moveValue);
            chronicleMap.remove(key);
            byte[] oldExpireKey = getExpireKey(key);
            byte[] timestampBytes = chronicleMap.get(oldExpireKey);
            if (timestampBytes != null) {
                chronicleMap.put(getExpireKey(newKey), timestampBytes);
                chronicleMap.remove(oldExpireKey);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] get(byte[] key) {
        lock.lock();
        try {
            if (isExpired(key)) {
                chronicleMap.remove(key);
            }
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
            chronicleMap.remove(getExpireKey(key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeExpire(byte[] key) {
        byte[] expireKey = getExpireKey(key);
        chronicleMap.remove(expireKey);
    }

    @Override
    public boolean remove(byte[] key) {
        lock.lock();
        try {
            chronicleMap.remove(getExpireKey(key));
            return chronicleMap.remove(key) != null;
        } finally {
            lock.unlock();
        }
    }

    private byte[] compute(byte[] key, BinaryOperator<byte[]> fn) {
        if (isExpired(key)) {
            chronicleMap.remove(key);
        }
        return chronicleMap.compute(key, fn);
    }

    @Override
    public byte[] incrBy(byte[] key, long amount) {
        lock.lock();
        try {
            return this.compute(key, (k, oldVal) -> {
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
            byte[] value = this.get(key);
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
            byte[] value = this.get(key);
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
            byte[] value = this.get(key);
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
            byte[] value = this.get(key);
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
            this.compute(key, (k, oldVal) -> {
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
            byte[] value = this.get(key);
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

    @Override
    @SuppressWarnings("unchecked")
    public int lpush(byte[] key, byte[]... values) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            for (byte[] v : values) {
                list.addFirst(new BytesValue(v));
            }
            chronicleMap.put(key, SerializationUtils.serialize(list));
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int rpush(byte[] key, byte[]... values) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            LinkedList<BytesValue> list;
            list = value == null ? new LinkedList<>() : (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            for (byte[] v : values) {
                list.addLast(new BytesValue(v));
            }
            this.put(key, SerializationUtils.serialize(list));
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] lpop(byte[] key) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return null;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (list.isEmpty()) {
                return null;
            }
            byte[] result = list.removeFirst().getBytes();
            chronicleMap.put(key, SerializationUtils.serialize(list));
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] rpop(byte[] key) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return null;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (list.isEmpty()) {
                return null;
            }
            byte[] result = list.removeLast().getBytes();
            chronicleMap.put(key, SerializationUtils.serialize(list));
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int llen(byte[] key) {
        lock.lock();
        try {
            byte[] value = chronicleMap.get(key);
            if (value == null) {
                return 0;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] lrange(byte[] key, int start, int end) {
        lock.lock();
        try {
            byte[] value = this.get(key);
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
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] lindex(byte[] key, int index) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return null;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
            if (index < 0) {
                index = list.size() + index;
            }
            if (index < 0 || index >= list.size()) {
                return null;
            }
            return list.get(index).getBytes();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void lset(byte[] key, int index, byte[] value) {
        lock.lock();
        try {
            byte[] value1 = this.get(key);
            if (value1 == null) {
                return;
            }
            LinkedList<BytesValue> list = (LinkedList<BytesValue>) SerializationUtils.deserialize(value1);
            if (index < 0) {
                index = list.size() + index;
            }
            if (index < 0 || index >= list.size()) {
                return;
            }
            list.set(index, new BytesValue(value));
            chronicleMap.put(key, SerializationUtils.serialize(list));
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int lrem(byte[] key, int count, byte[] value) {
        lock.lock();
        try {
            byte[] value1 = this.get(key);
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
            chronicleMap.put(key, SerializationUtils.serialize(list));
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int sadd(byte[] key, byte[]... values) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            HashSet<BytesKey> set;
            set = value == null ? new HashSet<>() : (HashSet<BytesKey>) SerializationUtils.deserialize(value);
            int count = 0;
            for (byte[] v : values) {
                boolean isNewElement = set.add(new BytesKey(v));
                if (isNewElement) {
                    count++;
                }
            }
            chronicleMap.put(key, SerializationUtils.serialize(set));
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] smembers(byte[] key) {
        lock.lock();
        try {
            byte[] value = this.get(key);
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
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean sismember(byte[] key, byte[] value) {
        lock.lock();
        try {
            byte[] got = this.get(key);
            if (got == null) {
                return false;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(got);
            return set.contains(new BytesKey(value));
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int scard(byte[] key) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return 0;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(value);
            return set.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sdiff(byte[]... keys) {
        lock.lock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = this.get(key);
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
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sinter(byte[]... keys) {
        lock.lock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = this.get(key);
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
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[][] sunion(byte[]... keys) {
        lock.lock();
        try {
            HashSet<BytesKey> set = new HashSet<>();
            for (byte[] key : keys) {
                byte[] value = this.get(key);
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
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int smove(byte[] source, byte[] destination, byte[] value) {
        lock.lock();
        try {
            byte[] sourceValue = this.get(source);
            if (sourceValue == null) {
                return 0;
            }
            HashSet<BytesKey> set = (HashSet<BytesKey>) SerializationUtils.deserialize(sourceValue);
            if (set.remove(new BytesKey(value))) {
                byte[] destinationValue = this.get(destination);
                HashSet<BytesKey> set1;
                if (destinationValue == null) {
                    set1 = new HashSet<>();
                } else {
                    set1 = (HashSet<BytesKey>) SerializationUtils.deserialize(destinationValue);
                }
                boolean result = set1.add(new BytesKey(value));
                chronicleMap.put(source, SerializationUtils.serialize(set));
                chronicleMap.put(destination, SerializationUtils.serialize(set1));
                return result ? 1 : 0;
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int srem(byte[] key, byte[]... values) {
        lock.lock();
        try {
            byte[] value = this.get(key);
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
                chronicleMap.remove(key);
            } else {
                chronicleMap.put(key, SerializationUtils.serialize(set));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int strlen(byte[] key) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return 0;
            }
            return new String(value, StandardCharsets.UTF_8).length();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int setrange(byte[] key, byte[] offset, byte[] val) {
        lock.lock();
        try {
            int offsetPosition = Integer.parseInt(new String(offset, StandardCharsets.UTF_8));
            byte[] oldVal = this.get(key);
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
            chronicleMap.put(key, newVal);
            return newValLength;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[][] mget(byte[]... keys) {
        lock.lock();
        try {
            byte[][] result = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                byte[] key = keys[i];
                byte[] got = this.get(key);
                result[i] = got;
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int zadd(final byte[] key, @NonNull final AbstractMap.SimpleEntry<Double, BytesKey>[] members, final int flags) {
        boolean xx = (flags & FLAG_XX) != 0;
        boolean nx = (flags & FLAG_NX) != 0;
        boolean lt = (flags & FLAG_LT) != 0;
        boolean gt = (flags & FLAG_GT) != 0;
        boolean ch = (flags & FLAG_CH) != 0;

        // Track both to eliminate conditional branch
        int added = 0, changed = 0;

        lock.lock();
        try {
            byte[] value = this.get(key);
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
                if (nx || (lt && newScore >= currScore) || (gt && newScore <= currScore)) {
                    continue;
                }
                if (!newScore.equals(currScore)) {
                    zSet.removeByKey(member.getValue());
                    zSet.add(member);
                    ++changed;
                }
            }
            chronicleMap.put(key, SerializationUtils.serialize(zSet));
            return ch ? changed : added;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Double zincrby(byte[] key, Double incr, BytesKey e, int flags) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            ZSet zSet;
            zSet = value == null ? new ZSet() : (ZSet) SerializationUtils.deserialize(value);
            Double currentScore = zSet.getScore(e);
            if (currentScore == null) {
                if ((flags & FLAG_XX) != 0) {
                    return null;
                }
                currentScore = incr;
                zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
                chronicleMap.put(key, SerializationUtils.serialize(zSet));
                return currentScore;
            }
            if ((flags & FLAG_NX) != 0) {
                return null;
            }
            if ((flags & FLAG_LT) != 0 && (incr >= 0 || currentScore.isInfinite())) {
                return null;
            }
            if ((flags & FLAG_GT) != 0 && (incr <= 0 || currentScore.isInfinite())) {
                return null;
            }
            zSet.remove(new AbstractMap.SimpleEntry<>(currentScore, e));
            currentScore += incr;
            zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
            chronicleMap.put(key, SerializationUtils.serialize(zSet));
            return currentScore;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return null;
            }
            ZSet zset = (ZSet) SerializationUtils.deserialize(value);
            return zset.getScore(new BytesKey(member));
        } finally {
            lock.unlock();
        }
    }

    public byte[] getrange(byte[] key, byte[] start, byte[] end) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            int startInt = Integer.parseInt(new String(start, StandardCharsets.UTF_8));
            int endInt = Integer.parseInt(new String(end, StandardCharsets.UTF_8));

            // convert negative indexes to positive ones
            if (startInt < 0 && endInt < 0 && startInt > endInt) {
                return null;
            }
            if (startInt < 0) startInt = value.length + startInt;
            if (endInt < 0) endInt = value.length + endInt;
            if (startInt < 0) startInt = 0;
            if (endInt < 0) endInt = 0;
            if (endInt >= value.length) endInt = value.length - 1;

            byte[] result;
            if (startInt > endInt || value.length == 0) {
                result = new String("").getBytes();
            } else {
                result = Arrays.copyOfRange(value, startInt, endInt + 1);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public byte[] incrbyfloat(byte[] key, double amount) {
        lock.lock();
        try {
            return this.compute(key, (k, oldVal) -> {
                double curVal = 0L;
                if (oldVal != null) {
                    curVal = Double.parseDouble(new String(oldVal, StandardCharsets.UTF_8));
                }
                curVal = curVal + amount;
                return Double.toString(curVal).getBytes(StandardCharsets.UTF_8);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void mset(byte[]... keys) {
        lock.lock();
        try {
            for (int i = 0; i < keys.length; i += 2) {
                this.put(keys[i], keys[i + 1]);
            }
        } finally {
            lock.unlock();
        }
    }

    public byte[] decrby(byte[] key, long amount) {
        lock.lock();
        try {
            return this.compute(key, (k, oldVal) -> {
                long curVal = 0L;
                if (oldVal != null) {
                    curVal = Long.parseLong(new String(oldVal, StandardCharsets.UTF_8));
                }
                curVal = curVal - amount;
                return Long.toString(curVal).getBytes(StandardCharsets.UTF_8);
            });
        } finally {
            lock.unlock();
        }
    }

    public byte[] substr(byte[] key, int startInt, int endInt) {
        lock.lock();
        try {
            byte[] value = this.get(key);
            if (value == null) {
                return null;
            }
            String valueStr = new String(value, StandardCharsets.UTF_8);

            // Convert negative indexes to positive ones
            if (startInt < 0 && endInt < 0 && startInt > endInt) {
                return null;
            }
            if (startInt < 0) startInt = valueStr.length() + startInt;
            if (endInt < 0) endInt = valueStr.length() + endInt;
            if (startInt < 0) startInt = 0;
            if (endInt < 0) endInt = 0;
            if (endInt >= valueStr.length()) endInt = valueStr.length() - 1;

            byte[] result;
            if (startInt > endInt) {
                result = "".getBytes();
            } else {
                result = valueStr.substring(startInt, endInt + 1).getBytes(StandardCharsets.UTF_8);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
