package com.jinyframework.keva.server.storage;

import java.util.Map;
import java.util.Set;

public interface KevaStore {
    Object get(String key);

    void put(String key, Object value);

    void putAll(Map<String, Object> m);

    void remove(String key);

    void expire(String key, long expireTimeInMilliSecond);

    Set<Map.Entry<String, Object>> entrySetCopy();
}
