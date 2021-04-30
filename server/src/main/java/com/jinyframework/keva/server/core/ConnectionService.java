package com.jinyframework.keva.server.core;

public interface ConnectionService {
    void handleConnection(ServerSocket serverSocket);

    long getCurrentConnectedClients();

    Runnable getHeartbeatRunnable(long sockTimeout);
}
