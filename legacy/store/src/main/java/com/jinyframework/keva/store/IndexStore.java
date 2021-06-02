package com.jinyframework.keva.store;

public interface IndexStore {
    void put(String k, Long v);

    Long get(String k);

    void remove(String k);

    int getCollisions();

    int getLoad();

    void outputStats();

    void reset();
}
