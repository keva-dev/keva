package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceFactory;
import com.jinyframework.keva.server.command.CommandService;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionService {
    private final CommandService commandService = ServiceFactory.commandService();

    private final Map<String, KevaSocket> socketMap = new ConcurrentHashMap<>();

    public void handleConnection(KevaSocket kevaSocket) {
        try {
            socketMap.put(kevaSocket.getId(), kevaSocket);
            val socket = kevaSocket.getSocket();
            log.info("{} {} connected", socket.getRemoteSocketAddress(), kevaSocket.getId());

            @Cleanup
            val socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            @Cleanup
            val socketOut = new PrintWriter(socket.getOutputStream());
            while (kevaSocket.isAlive()) {
                val line = socketIn.readLine();
                if (line == null || line.isEmpty()) {
                    continue;
                }
                kevaSocket.getLastOnlineLong().set(System.currentTimeMillis());
                log.info("{} sent {}", kevaSocket.getId(), line);
                commandService.handleCommand(socketOut, line);
            }
        } catch (SocketException ignored) {
            socketMap.remove(kevaSocket.getId());
            log.info("{} {} disconnected", kevaSocket.getSocket().getRemoteSocketAddress(), kevaSocket.getId());
        } catch (Exception e) {
            log.error("Error while handling socket {}: {}", kevaSocket.getId(), e);
        }
    }

    public long getCurrentConnectedClients() {
        return socketMap.size();
    }

    public Runnable getHeartbeatRunnable(long sockTimeout) {
        return () -> {
            log.info("Running heartbeat");
            val now = System.currentTimeMillis();
            socketMap.values().forEach(kevaSocket -> {
                if (kevaSocket.getLastOnline() + sockTimeout < now) {
                    kevaSocket.getAlive().set(false);
                    try {
                        kevaSocket.getSocket().close();
                    } catch (IOException e) {
                        log.error("Error while closing socket {}: {}", kevaSocket.getId(), e);
                    }
                    socketMap.remove(kevaSocket.getId());
                    log.info("{} {} closed from timeout", kevaSocket.getSocket().getRemoteSocketAddress(), kevaSocket.getId());
                }
            });
        };
    }
}
