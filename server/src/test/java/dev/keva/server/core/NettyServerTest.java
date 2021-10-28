package dev.keva.server.core;

import dev.keva.server.config.ConfigHolder;
import dev.keva.server.config.util.PortUtil;
import dev.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.TimeUnit;

@Slf4j
@DisplayName("Netty Server")
public class NettyServerTest extends AbstractServerTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();

    @BeforeAll
    static void startServer() throws Exception {
        server = new NettyServer(ConfigHolder.defaultBuilder()
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
        TimeUnit.SECONDS.sleep(6);

        client = new SocketClient(host, port);
        client.connect();
    }

    @AfterAll
    static void stop() {
        client.disconnect();
        server.shutdown();
    }
}
