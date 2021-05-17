package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.server.core.ServerSocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SocketMapInstance {
    private SocketMapInstance() {
    }

    private static final class SocketHashMapHolder {
        private static final ConcurrentHashMap<String, ServerSocket> INSTANCE = new ConcurrentHashMap<>();
    }

    public static ConcurrentMap<String, ServerSocket> getSocketHashMap() {
        return SocketHashMapHolder.INSTANCE;
    }
}
