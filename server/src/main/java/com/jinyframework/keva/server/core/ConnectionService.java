package com.jinyframework.keva.server.core;

public interface ConnectionService {
    void handleConnection(KevaSocket kevaSocket);

    long getCurrentConnectedClients();

    Runnable getHeartbeatRunnable(long sockTimeout);
}
