package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
public class RestartServerTest {
    static String host = "localhost";

    @Test
    public void whenRestart_withValueSet_verifyValueExist() throws InterruptedException, IOException {
        int port = PortUtil.getAvailablePort();
        NettyServer server = new NettyServer(ConfigHolder.defaultBuilder()
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

        SocketClient client = new SocketClient(host, port);
        client.connect();

        assertEquals("1", client.exchange("set abc " + port));

        server.shutdown();
        // wait for server to shutdown
        TimeUnit.SECONDS.sleep(2);
        assertNull(client.exchange("PING"));


        new Thread(() -> {
            try {
                server.run(false);
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(2);
        client.connect();
        assertEquals(String.valueOf(port), client.exchange("get abc"));
    }
}
