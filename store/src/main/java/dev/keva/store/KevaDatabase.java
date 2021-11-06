package dev.keva.store;

import java.util.concurrent.locks.Lock;

public interface KevaDatabase {
    Lock getLock();

    void put(byte[] key, byte[] val);

    byte[] get(byte[] key);

    boolean remove(byte[] key);
}
