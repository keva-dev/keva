package dev.keva.core.command;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.server.KevaServer;
import dev.keva.core.server.Server;
import dev.keva.core.utils.PortUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import redis.clients.jedis.Jedis;

@Slf4j
public class BaseCommandTest {
    protected static final String host = "localhost";
    protected static final int port = PortUtil.getAvailablePort();
    protected static Jedis jedis;
    protected static Server server;

    @BeforeAll
    static void startServer() {
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
        server.await();

        jedis = new Jedis(host, port);
        jedis.auth("keva-auth");
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
