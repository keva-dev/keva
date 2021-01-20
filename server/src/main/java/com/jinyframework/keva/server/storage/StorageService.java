package com.jinyframework.keva.server.storage;

public final class StorageService {
    static HashMapStorage<String, String> stringStringHashMap = new HashMapStorage<>();

    private StorageService() {
    }

    public static HashMapStorage<String, String> getStringStringStore() {
        return stringStringHashMap;
    }
}
