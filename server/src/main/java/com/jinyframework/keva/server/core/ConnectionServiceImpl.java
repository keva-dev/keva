package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceFactory;
import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.storage.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Map;

@Slf4j
public class ConnectionServiceImpl implements ConnectionService {
    private final CommandService commandService = ServiceFactory.getCommandService();

    private final Map<String, ServerSocket> socketMap = StorageFactory.getSocketHashMap();

    @Override
    public void handleConnection(ServerSocket serverSocket) {
        val socketId = serverSocket.getId();
        try {
            socketMap.put(socketId, serverSocket);
            val socket = serverSocket.getSocket();
            val remoteAddr = socket.getRemoteSocketAddress();
            log.info("{} {} connected", remoteAddr, socketId);

            try (val socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                try (val socketOut = new PrintWriter(socket.getOutputStream())) {
                    while (serverSocket.isAlive()) {
                        val line = socketIn.readLine();
                        if (line == null) {
                            socketMap.remove(socketId);
                            log.info("{} {} disconnected", remoteAddr, socketId);
                            break;
                        }
                        serverSocket.getLastOnlineLong().set(System.currentTimeMillis());
                        log.info("{} sent {}", serverSocket.getId(), line);
                        commandService.handleCommand(socketOut, line);
                    }
                }
            }
        } catch (SocketException e) {
            log.debug("SocketException {}: {}", socketId, e);
        } catch (Exception e) {
            log.error("Error while handling socket {}: {}", socketId, e);
        }
    }

    @Override
    public long getCurrentConnectedClients() {
        return socketMap.size();
    }

    @Override
    public Runnable getHeartbeatRunnable(long sockTimeout) {
        return () -> {
            log.info("Running heartbeat");
            val now = System.currentTimeMillis();
            socketMap.values().forEach(serverSocket -> {
                if (serverSocket.getLastOnline() + sockTimeout < now) {
                    serverSocket.getAlive().set(false);
                    try {
                        serverSocket.getSocket().close();
                    } catch (IOException e) {
                        log.error("Error while closing socket {}: {}", serverSocket.getId(), e);
                    }
                    socketMap.remove(serverSocket.getId());
                    log.info("{} {} closed from timeout", serverSocket.getSocket().getRemoteSocketAddress(), serverSocket.getId());
                }
            });
        };
    }
}
