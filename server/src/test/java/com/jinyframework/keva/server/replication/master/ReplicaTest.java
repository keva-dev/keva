package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.util.PortUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public final class ReplicaTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();
    private static IServer server;
    private static Replica rep;

    private ReplicaTest() {
    }

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
        TimeUnit.SECONDS.sleep(10);

        rep = new Replica(host, port);
        rep.connect();
        new Thread(rep.commandRelayTask()).start();
    }

    @AfterAll
    static void stop() {
        server.shutdown();
    }

    @Test
    @Timeout(2)
    void clientSend() throws Exception {
        assertEquals("PONG", rep.send("PING").get());
        assertEquals("null", rep.send("get abc").get());
        assertEquals("1", rep.send("set abc xyz").get());
    }

    @Test
    @Timeout(2)
    void repSendWhenCommandIsBuffered() throws Exception {
        rep.buffer("set a b");
        rep.buffer("set a c");
        rep.buffer("set a d");
        TimeUnit.MILLISECONDS.sleep(100);
        assertEquals("d", rep.send("get a").get());
    }
}
