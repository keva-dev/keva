package dev.keva.server;

import lombok.val;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTest {

    public static final String[] ARGS = new String[0];

    @Test
    void testMain() throws Exception {
        new Thread(() -> Application.main(ARGS)).start();
        TimeUnit.SECONDS.sleep(5);

        Jedis jedis = new Jedis("localhost", 6767);
        val pong = jedis.ping();
        assertEquals("PONG", pong);
    }
}
