package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DisplayName("BIO Server")
public class ServerTest extends AbstractServerTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();

    private static void deleteFile(String name) {
        val conf = new File(name);
        if (conf.exists()) {
            val deleted = conf.delete();
            if (!deleted) {
                log.warn("Failed to delete file {}", name);
            }
        }
    }

    @BeforeAll
    static void startServer() throws Exception {
        deleteFile("./keva.test.properties");
        deleteFile("./KevaData");
        deleteFile("./KevaDataIndex");

        server = new Server(ConfigHolder.defaultBuilder()
                                        // TODO: check why adding snapshotEnabled = false make test fail
                                        .snapshotEnabled(false)
                                        .hostname(host)
                                        .port(port)
                                        .build());
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(1);

        client = new SocketClient(host, port);
        client.connect();
    }

    @AfterAll
    static void stop() {
        client.disconnect();
        server.shutdown();
    }
}
