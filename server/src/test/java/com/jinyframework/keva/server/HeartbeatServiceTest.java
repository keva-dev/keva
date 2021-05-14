package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class HeartbeatServiceTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();
    static Server server;
    static long heartbeatTimeout = 500;

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server(ConfigHolder.defaultBuilder()
                .hostname(host)
                .port(port)
                .heartbeatEnabled(true)
                .heartbeatTimeout(heartbeatTimeout)
                .build());
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(1);

    }

    @AfterAll
    static void stop() throws Exception {
        server.shutdown();
    }

    @Test
    void alive() {
        val client = new SocketClient(host, port);
        try {
            client.connect();

            String pong = client.exchange("ping");
            assertEquals("PONG", pong);
            TimeUnit.MILLISECONDS.sleep(100);
            pong = client.exchange("ping");
            assertEquals("PONG", pong);
            TimeUnit.MILLISECONDS.sleep(300);
            pong = client.exchange("ping");
            assertEquals("PONG", pong);
        } catch (Exception e) {
            fail(e);
        }
        client.disconnect();
    }

    @Test
    void timeout() {
        val client = new SocketClient(host, port);
        try {
            client.connect();

            String pong = client.exchange("ping");
            assertEquals("PONG", pong);

            // wait for timeout
            TimeUnit.MILLISECONDS.sleep(heartbeatTimeout);

            // wait for next heartbeat cycle to run
            TimeUnit.MILLISECONDS.sleep(heartbeatTimeout / 2);

            // wait for it to finish running
            TimeUnit.MILLISECONDS.sleep(100);

            pong = client.exchange("ping");
            assertNull(pong);
        } catch (Exception e) {
            fail(e);
        }
        client.disconnect();
    }
}
