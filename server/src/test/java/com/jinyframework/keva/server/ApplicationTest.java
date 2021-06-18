package com.jinyframework.keva.server;

import com.jinyframework.keva.server.util.SocketClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    public static final String[] ARGS = new String[0];

    @Test
    void testMain() throws Exception {
        new Thread(() -> Application.main(ARGS)).start();
        TimeUnit.SECONDS.sleep(5);
        final SocketClient client = new SocketClient("localhost", 6767);
        client.connect();
        final String pong = client.exchange("PING");
        assertEquals("PONG", pong);
    }
}
