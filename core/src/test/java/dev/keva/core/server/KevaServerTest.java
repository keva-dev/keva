package dev.keva.core.server;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.utils.PortUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

@Slf4j
public class KevaServerTest extends AbstractServerTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();

    @BeforeAll
    static void startServer() throws Exception {
        val config = KevaConfig.builder()
                .persistence(false)
                .aof(false)
                .hostname(host)
                .port(port)
                .password("keva-auth")
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
        jedis.auth("keva-auth");
        subscriber = new Jedis(host, port);
        subscriber.auth("keva-auth");
    }

    @AfterAll
    static void stop() {
        jedis.disconnect();
        server.shutdown();
    }

    @BeforeEach
    void reset() {
        server.clear();
    }
}
