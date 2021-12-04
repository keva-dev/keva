package dev.keva.store;

import java.util.concurrent.locks.Lock;

public interface KevaDatabase {
    Lock getLock();

    void clear();

    void put(byte[] key, byte[] val);

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

}
