package dev.keva.app;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTest {

    public static int port = getAvailablePort();
    public static final String[] ARGS = {"--p", Integer.toString(port)};

    @Test
    void testMain() throws Exception {
        new Thread(() -> Application.main(ARGS)).start();
        TimeUnit.SECONDS.sleep(5);

        val jedis = new Jedis("localhost", port);
        val pong = jedis.ping();
        assertEquals("PONG", pong);
    }

    @SneakyThrows
    public static int getAvailablePort() {
        final int port;
        try (val serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        return port;
    }
}
