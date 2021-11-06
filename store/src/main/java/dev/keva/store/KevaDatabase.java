package dev.keva.store;

public interface KevaDatabase {
    void lock();

    void unlock();

    void put(byte[] key, byte[] val);

    byte[] get(byte[] key);

    boolean remove(byte[] key);
}
