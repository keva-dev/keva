package com.jinyframework.keva.server;

import com.jinyframework.keva.server.util.SocketClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @Test
    void main() throws Exception {
        String[] args = new String[0];
        new Thread(() -> Application.main(args)).start();
        TimeUnit.SECONDS.sleep(1);
        SocketClient client = new SocketClient("localhost", 6767);
        client.connect();
        String pong = client.exchange("PING");
        assertEquals("PONG", pong);
    }
}