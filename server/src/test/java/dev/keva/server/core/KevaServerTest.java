package dev.keva.server.core;

import dev.keva.server.config.KevaConfig;
import dev.keva.server.utils.PortUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.Jedis;

import lombok.val;

import java.util.concurrent.TimeUnit;

@Slf4j
public class KevaServerTest extends AbstractServerTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();

    @BeforeAll
    static void startServer() throws Exception {
        val config = KevaConfig.custom()
                .persistence(false)
                .hostname(host)
                .port(port)
                .build();
        server = KevaServer.of(config);

        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(2);

        jedis = new Jedis(host, port);
        subscriber = new Jedis(host, port);
    }

    @AfterAll
    static void stop() {
        jedis.disconnect();
        server.shutdown();
    }
}
