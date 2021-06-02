package com.jinyframework.keva.engine;

public interface KevaMap<K, V> {
    V get(Object key);
    V put(K key, V value);
    V remove(Object key);
    void close();
}
