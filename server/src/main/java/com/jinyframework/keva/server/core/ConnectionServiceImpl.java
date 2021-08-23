package com.jinyframework.keva.server.core;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ConnectionServiceImpl implements ConnectionService {

    private ConcurrentHashMap<String, ClientInfo> clients = new ConcurrentHashMap<>();

    @Override
    public long getCurrentConnectedClients() {
        return clients.size();
    }

    @Override
    public ConcurrentMap<String, ClientInfo> getClients() {
        return clients;
    }
}
