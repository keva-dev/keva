package dev.keva.store;

import dev.keva.util.hashbytes.BytesKey;

import java.util.AbstractMap;
import java.util.concurrent.locks.Lock;

public interface KevaDatabase {
    Lock getLock();

    void flush();

    void put(byte[] key, byte[] val);

    void removeExpire(byte[] key);

    void expireAt(byte[] key, long timestampInMillis);

    boolean rename(byte[] key, byte[] newKey);

    byte[] get(byte[] key);

    boolean remove(byte[] key);

    byte[] incrBy(byte[] key, long amount);

    byte[] hget(byte[] key, byte[] field);

    byte[][] hgetAll(byte[] key);

    byte[][] hkeys(byte[] key);

    byte[][] hvals(byte[] key);

    void hset(byte[] key, byte[] field, byte[] value);

    boolean hdel(byte[] key, byte[] field);

    int lpush(byte[] key, byte[]... values);

    int rpush(byte[] key, byte[]... values);

    byte[] lpop(byte[] key);

    byte[] rpop(byte[] key);

    int llen(byte[] key);

    byte[][] lrange(byte[] key, int start, int stop);

    byte[] lindex(byte[] key, int index);

    void lset(byte[] key, int index, byte[] value);

    int lrem(byte[] key, int count, byte[] value);

    int sadd(byte[] key, byte[]... values);

    byte[][] smembers(byte[] key);

    boolean sismember(byte[] key, byte[] value);

    int scard(byte[] key);

    byte[][] sdiff(byte[]... keys);

    byte[][] sinter(byte[]... keys);

    byte[][] sunion(byte[]... keys);

    int smove(byte[] source, byte[] destination, byte[] value);

    int srem(byte[] key, byte[]... values);

    int strlen(byte[] key);

    int setrange(byte[] key, byte[] offset, byte[] val);

    byte[][] mget(byte[]... keys);

    int zadd(byte[] key, AbstractMap.SimpleEntry<Double, BytesKey>[] members, int flags);

    Double zincrby(byte[] key, Double score, BytesKey e, int flags);

    Double zscore(byte[] key, byte[] member);

    byte[] decrby(byte[] key, long amount);

    byte[] getrange(byte[] key, byte[] start, byte[] end);

    byte[] incrbyfloat(byte[] key, double amount);

    void mset(byte[]... key);

    byte[] substr(byte[] key, int startInt, int endInt);
}
