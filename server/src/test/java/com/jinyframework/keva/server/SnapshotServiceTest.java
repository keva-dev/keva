package com.jinyframework.keva.server;

import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.core.SnapshotConfig;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.val;
import lombok.var;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

import static com.jinyframework.keva.server.util.PortUtil.getAvailablePort;
import static org.junit.jupiter.api.Assertions.*;

public class SnapshotServiceTest {
    static String host = "localhost";
    static String snapInterval = "PT2S";

    Server startServer(int port) throws Exception {
        val snapshotConfig = SnapshotConfig.builder()
                .snapshotInterval(snapInterval)
                .recoveryPath("./dump.keva")
                .build();
        val server = Server.builder()
                .host(host)
                .port(port)
                .snapshotConfig(snapshotConfig)
                .build();
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                fail(e);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(1);
        return server;
    }

    void stop(Server server) throws Exception {
        server.shutdown();
    }

    void deleteFile(String filePath) {
        val f = new File(filePath);
        if (f.exists()) {
            val delete = f.delete();
            if (!delete) {
                fail("Delete dump file failed");
            }
        }
    }

    @Test
    void save() {
        sync(getAvailablePort());
    }

    void sync(int port) {
        deleteFile("./dump.keva");
        Server server = null;
        try {
            server = startServer(port);
        } catch (Exception e) {
            fail(e);
        }
        val client = new SocketClient(host, port);
        try {
            client.connect();

            var success = client.exchange("set a b");
            assertEquals("1", success);
            success = client.exchange("set b c");
            assertEquals("1", success);
            success = client.exchange("set c d");
            assertEquals("1", success);

            // Wait for snap service to start
            TimeUnit.SECONDS.sleep(2);
            // Wait for snap service to finish
            TimeUnit.MILLISECONDS.sleep(100);

            val fileInputStream = new FileInputStream("./dump.keva");
            val available = fileInputStream.available();
            assertTrue(available > 0);
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

            var success = client.exchange("get a");
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
