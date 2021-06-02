package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.engine.KevaMap;

public class StorageServiceImpl implements StorageService {
    private static KevaMap<String, String> engine;

    @Override
    public void setEngine(KevaMap<String, String> engine) {
        StorageServiceImpl.engine = engine;
    }

    @Override
    public String put(String key, String val) {
        return engine.put(key, val);
    }

    @Override
    public String get(String key) {
        return engine.get(key);
    }

    @Override
    public String remove(String key) {
        return engine.remove(key);
    }
}
