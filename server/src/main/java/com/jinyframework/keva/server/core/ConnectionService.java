package com.jinyframework.keva.server.core;

import java.util.concurrent.ConcurrentMap;

public interface ConnectionService {
    long getCurrentConnectedClients();

    ConcurrentMap<String, ClientInfo> getClients();

    void init();
}
