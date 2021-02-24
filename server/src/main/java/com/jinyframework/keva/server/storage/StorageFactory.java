package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.server.core.KevaSocket;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

@Setter
public final class StorageFactory {
    private static KevaStore kevaStore;
    private static ConcurrentHashMap<String, KevaSocket> socketHashMap;

    public synchronized static KevaStore getKevaStore() {
        if (kevaStore == null) {
            kevaStore = new KevaStoreImpl();
        }

        return kevaStore;
    }

    public synchronized static ConcurrentHashMap<String, KevaSocket> getSocketHashMap() {
        if (socketHashMap == null) {
            socketHashMap = new ConcurrentHashMap<>();
        }

        return socketHashMap;
    }
}
