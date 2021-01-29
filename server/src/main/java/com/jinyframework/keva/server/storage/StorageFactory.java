package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.server.core.KevaSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StorageFactory {

    private StorageFactory() {
    }

    public static Map<String, String> hashStore() {
        return StringStringHashMapHolder.stringStringHashMap;
    }

    public static Map<String, KevaSocket> socketStore() {
        return SocketHashMapHolder.socketHashMap;
    }

    private static final class StringStringHashMapHolder {
        static final ConcurrentHashMap<String, String> stringStringHashMap = new ConcurrentHashMap<>();
    }

    private static final class SocketHashMapHolder {
        static final ConcurrentHashMap<String, KevaSocket> socketHashMap = new ConcurrentHashMap<>();
    }
}
