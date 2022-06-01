package dev.keva.core.server;

import dev.keva.core.config.KevaConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import redis.clients.jedis.Jedis;

import java.nio.file.Path;

import static dev.keva.core.utils.PortUtil.getAvailablePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class AOFTest {
    static String host = "localhost";

    @TempDir
    static Path tempDir;

    Server startServer(int port) throws Exception {
        val config = KevaConfig.builder()
                .hostname(host)
                .port(port)
                .persistence(false)
                .aof(true)
                .aofInterval(1000)
                .workDirectory(tempDir.toString())
                .build();
        val server = KevaServer.of(config);
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                System.exit(1);
            }
        }).start();

        server.await();
        return server;
    }

    void stop(Server server) {
        server.shutdown();
    }

    @Test
    @Timeout(30)
    void save() throws InterruptedException {
        sync(getAvailablePort());
    }

    void sync(int port) {
        Server server = null;
        try {
            server = startServer(port);
        } catch (Exception e) {
            fail(e);
        }
        val jedis = new Jedis(host, port);
        try {
            String success = jedis.set("a", "b");
            assertEquals("OK", success);
            success = jedis.set("b", "c");
            assertEquals("OK", success);
            success = jedis.set("c", "d");
            assertEquals("OK", success);
        } catch (Exception e) {
            fail(e);
        }
        jedis.disconnect();

        try {
            stop(server);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(20)
    void recover() {
        val port = getAvailablePort();
        Server server = null;
        try {
            server = startServer(port);
        } catch (Exception e) {
            fail(e);
        }

        val jedis = new Jedis(host, port);
        try {
            String success = jedis.get("a");
            assertEquals("b", success);
            success = jedis.get("b");
            assertEquals("c", success);
            success = jedis.get("c");
            assertEquals("d", success);
        } catch (Exception e) {
            fail(e);
        }

        jedis.disconnect();
        try {
            stop(server);
        } catch (Exception e) {
            fail(e);
        }
    }
}
