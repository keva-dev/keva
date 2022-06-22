package dev.keva.storage;

import java.util.Set;
import java.util.concurrent.locks.Lock;

public interface KevaDatabase {
    Lock getLock();

    void flush();

    byte[] get(byte[] key);

    void put(byte[] key, byte[] val);

    boolean remove(byte[] key);

    boolean rename(byte[] key, byte[] newKey);

    Set<byte[]> keySet();

    void setExpiration(byte[] key, long timestampInMillis);

    void removeExpire(byte[] key);
}
