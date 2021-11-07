package dev.keva.store;

import java.util.concurrent.locks.Lock;
import java.util.function.BinaryOperator;

public interface KevaDatabase {
    Lock getLock();

    void put(byte[] key, byte[] val);

    byte[] get(byte[] key);

    boolean remove(byte[] key);

    byte[] incrBy(byte[] key, long amount);
}
