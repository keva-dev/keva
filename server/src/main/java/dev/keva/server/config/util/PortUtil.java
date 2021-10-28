package dev.keva.server.config.util;

import lombok.SneakyThrows;
import lombok.val;

import java.net.ServerSocket;

public final class PortUtil {
    @SneakyThrows
    public static int getAvailablePort() {
        final int port;
        try (val serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        return port;
    }
}
