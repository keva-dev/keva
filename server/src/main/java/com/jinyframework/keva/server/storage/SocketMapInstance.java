package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.server.core.ServerSocket;

import java.util.concurrent.ConcurrentHashMap;

public class SocketMapInstance {
    private static final class SocketHashMapHolder {
        private static final ConcurrentHashMap<String, ServerSocket> INSTANCE = new ConcurrentHashMap<>();
    }

    public static ConcurrentHashMap<String, ServerSocket> getSocketHashMap() {
        return SocketHashMapHolder.INSTANCE;
    }
}
