package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.engine.KevaMap;

public interface StorageService {
    public void setEngine(KevaMap<String, String> engine);

    public String put(String key, String val);

    public String get(String key);

    public String remove(String key);
}
