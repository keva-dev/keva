package dev.keva.store;

import java.util.concurrent.locks.Lock;
import java.util.function.BinaryOperator;

public interface KevaDatabase {
    Lock getLock();

    void flush();

    byte[] get(byte[] key);

    void put(byte[] key, byte[] val);

    boolean remove(byte[] key);

    byte[] compute(byte[] key, BinaryOperator<byte[]> fn);

    boolean rename(byte[] key, byte[] newKey);

    void expireAt(byte[] key, long timestampInMillis);

    void removeExpire(byte[] key);
}
