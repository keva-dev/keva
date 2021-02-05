package com.jinyframework.keva.server.util;

import lombok.SneakyThrows;
import lombok.val;

import java.net.ServerSocket;

public final class PortUtil {
    private PortUtil() {
    }

    @SneakyThrows
    public static int getAvailablePort() {
        val serverSocket = new ServerSocket(0);
        val port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }
}
