package dev.keva.store;

import java.util.concurrent.locks.Lock;
import java.util.function.BinaryOperator;

public interface KevaDatabase {
    Lock getLock();

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
}
