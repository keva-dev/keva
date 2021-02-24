package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceFactory;
import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.storage.StorageFactory;
import lombok.Cleanup;
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

    private final Map<String, KevaSocket> socketMap = StorageFactory.getSocketHashMap();

    @Override
    public void handleConnection(KevaSocket kevaSocket) {
        val socketId = kevaSocket.getId();
        try {
            socketMap.put(socketId, kevaSocket);
            val socket = kevaSocket.getSocket();
            val remoteAddr = socket.getRemoteSocketAddress();
            log.info("{} {} connected", remoteAddr, socketId);

            @Cleanup
            val socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            @Cleanup
            val socketOut = new PrintWriter(socket.getOutputStream());
            while (kevaSocket.isAlive()) {
                val line = socketIn.readLine();
                if (line == null) {
                    socketMap.remove(socketId);
                    log.info("{} {} disconnected", remoteAddr, socketId);
                    break;
                }
                kevaSocket.getLastOnlineLong().set(System.currentTimeMillis());
                log.info("{} sent {}", kevaSocket.getId(), line);
                commandService.handleCommand(socketOut, line);
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
