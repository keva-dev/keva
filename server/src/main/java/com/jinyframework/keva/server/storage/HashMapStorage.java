package com.jinyframework.keva.server.storage;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class HashMapStorage<K, V> {
    private final ConcurrentMap<K, V> hashMap = new ConcurrentHashMap<>();

    public byte put(K key, V val) {
        try {
            hashMap.put(key, val);
            return 1;
        } catch (Exception e) {
            log.error("Failed to put {} {} with exception:{}", key, val, e);
            return 0;
        }
    }

    public Object get(K key) {
        try {
            return hashMap.get(key);
        } catch (Exception e) {
            log.error("Failed to get {} with exception:{}", key, e);
            return null;
        }
    }
}
