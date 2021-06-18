package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.storage.SocketMapInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ConnectionServiceImpl implements ConnectionService {

    private final Map<String, ServerSocket> socketMap = SocketMapInstance.getSocketHashMap();

    @Override
    public long getCurrentConnectedClients() {
        return socketMap.size();
    }
}
