package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.util.PortUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
public final class ReplicaTest {
    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();
    private static Server server;
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
        TimeUnit.SECONDS.sleep(6);

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
    void whenCmdIsBuffered_assertTheyWereSent() throws Exception {
        rep.buffer("set a b");
        TimeUnit.MILLISECONDS.sleep(100);
        assertEquals("b", rep.send("get a").get());
        rep.buffer("set a c");
        rep.buffer("set a d");
        TimeUnit.MILLISECONDS.sleep(100);
        assertEquals("d", rep.send("get a").get());
    }

    @Test
    @Timeout(10)
    void whenMasterNotAlive_assertBufferCleared() throws Exception {
        final int port = PortUtil.getAvailablePort();
        final Server server = new NettyServer(ConfigHolder.defaultBuilder()
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

        final Replica rep = new Replica(host, port);
        rep.connect();
        rep.buffer("set a b");
        rep.buffer("set b c");
        rep.buffer("set c d");
        assertEquals(true, rep.getCmdBuffer().contains("set a b"));
        final CompletableFuture<Object> lost = new CompletableFuture<>();
        server.shutdown();
        new Thread(rep.healthChecker(lost)).start();
        lost.whenComplete((res, ex) -> {
            assertFalse(rep.getCmdBuffer().contains("set a b"));
            assertFalse(rep.getCmdBuffer().contains("set b c"));
            assertFalse(rep.getCmdBuffer().contains("set c d"));
            assertFalse(rep.getCmdBuffer().isEmpty());
        });
    }
}
