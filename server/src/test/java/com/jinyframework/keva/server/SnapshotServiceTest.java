package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.config.ConfigManager;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.jinyframework.keva.server.util.PortUtil.getAvailablePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class SnapshotServiceTest {
    static String host = "localhost";

    Server startServer(int port) throws Exception {
        val config = ConfigHolder.builder()
                .hostname(host)
                .port(port)
                .snapshotEnabled(true)
                .snapshotLocation("./")
                .heapSize(8)
                .build();
        ConfigManager.setConfig(config);
        val server = new Server(config);
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(1);
        return server;
    }

    void stop(Server server) throws Exception {
        server.shutdown();
    }

    @Test
    void save() {
        sync(getAvailablePort());
    }

    void sync(int port) {
        Server server = null;
        try {
            server = startServer(port);
        } catch (Exception e) {
            fail(e);
        }
        val client = new SocketClient(host, port);
        try {
            client.connect();

            String success = client.exchange("set a b");
            assertEquals("1", success);
            success = client.exchange("set b c");
            assertEquals("1", success);
            success = client.exchange("set c d");
            assertEquals("1", success);

        } catch (Exception e) {
            fail(e);
        }
        client.disconnect();
        try {
            stop(server);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void recover() {
        sync(getAvailablePort());

        val port = getAvailablePort();
        Server server = null;
        try {
            server = startServer(port);
        } catch (Exception e) {
            fail(e);
        }

        val client = new SocketClient(host, port);
        try {
            client.connect();

            String success = client.exchange("get a");
            assertEquals("b", success);
            success = client.exchange("get b");
            assertEquals("c", success);
            success = client.exchange("get c");
            assertEquals("d", success);
        } catch (Exception e) {
            fail(e);
        }

        client.disconnect();
        try {
            stop(server);
        } catch (Exception e) {
            fail(e);
        }
    }
}
