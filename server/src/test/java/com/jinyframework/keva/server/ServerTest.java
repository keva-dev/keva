package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ServerTest {
    static String host = "localhost";
    static int port = 8787;
    static Server server;
    static SocketClient client;

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server(ConfigHolder.builder()
                .hostname(host)
                .port(port)
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

        client = new SocketClient(host, port);
        client.connect();
    }

    @AfterAll
    static void stop() throws Exception {
        client.disconnect();
        server.shutdown();
    }

    @Test
    void ping() {
        try {
            val pong = client.exchange("ping");
            assertEquals("PONG", pong);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void info() {
        try {
            val info = client.exchange("info");
            assertNotEquals("null", info);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSet() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            val getNull = client.exchange("get notexist");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
            assertEquals("null", getNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void del() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            val delAbc = client.exchange("del abc");
            val getAbcNull = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
            assertEquals("1", delAbc);
            assertEquals("null", getAbcNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetExpire() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            val expireAbc = client.exchange("expire abc 1000");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
            assertEquals("1", expireAbc);
            Thread.sleep(1500);
            val getAbcNull = client.exchange("get abc");
            assertEquals("null", getAbcNull);
        } catch (Exception e) {
            fail(e);
        }
    }
}
