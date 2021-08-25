package com.jinyframework.keva.store.legacy;

public interface LegacyIndexStore {
    void put(String k, Long v);

    Long get(String k);

    void remove(String k);

    int getCollisions();

    int getLoad();

    void outputStats();

    void reset();
}
