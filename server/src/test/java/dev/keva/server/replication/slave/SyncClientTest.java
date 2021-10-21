package dev.keva.server.replication.slave;

import dev.keva.server.config.ConfigHolder;
import dev.keva.server.core.NettyServer;
import dev.keva.server.util.PortUtil;
import dev.keva.server.util.SocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncClientTest {
    static final String host = "localhost";
    static final int port = PortUtil.getAvailablePort();
    static String masterId;
    static NettyServer server;

    @BeforeAll
    @SneakyThrows
    static void startServer() {
        Files.createDirectories(Path.of("./temptest/"));

        server = new NettyServer(ConfigHolder.defaultBuilder()
                                             .snapshotEnabled(true)
                                             .snapshotLocation("./temptest/")
                                             .hostname(host)
                                             .port(port)
                                             .build());
        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(Path.of("./temptest/", "dump.kdb"));
                    Files.deleteIfExists(Path.of("./temptest/"));
                } catch (IOException ioException) {
                    log.warn(ioException.getMessage());
                }
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(6);
        masterId = server.getReplicationService().getReplicationId();
    }

    @AfterAll
    @SneakyThrows
    static void shutdownServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    @Timeout(10)
    @Order(1)
    void fullSync() throws Exception {
        final SyncClient syncClient = new SyncClient(host, port);
        assertTrue(syncClient.connect());
        final CompletableFuture<Object> res = syncClient.sendSync("localhost", PortUtil.getAvailablePort());
        final String[] respContent = res.get().toString().split(" ");
        if ("F".equals(respContent[0])) {
            log.info("Performing full synchronization");
            final byte[] actual = Base64.getDecoder().decode(respContent[3]);
            final byte[] expected = Files.readAllBytes(Path.of("./temptest/dump.kdb"));
            assertArrayEquals(expected, actual);
            assertEquals(masterId, respContent[1]);
            assertEquals("0", respContent[2]);
        } else {
            fail(Arrays.toString(respContent));
        }
    }

    @Test
    @Timeout(10)
    @Order(2)
    void partialSync_noNewWrite() throws Exception {
        final SyncClient syncClient = new SyncClient(host, port);
        assertTrue(syncClient.connect());
        final CompletableFuture<Object> res = syncClient.sendSync("localhost", PortUtil.getAvailablePort(),
                                                                  masterId, 0);
        final String[] respContent = res.get().toString().split(" ");
        if ("P".equals(respContent[0])) {
            assertEquals(3, respContent.length);
            assertEquals(masterId, respContent[1]);
            assertEquals("0", respContent[2]);
        } else {
            fail(Arrays.toString(respContent));
        }
    }

    @Test
    @Timeout(10)
    @Order(99) // using same server instance so this should run last for now
    void partialSync_hasNewWrite() throws Exception {
        final SocketClient socketClient = new SocketClient(host, port);
        socketClient.connect();
        socketClient.exchange("SET a b");
        socketClient.exchange("SET b c");
        socketClient.exchange("SET c d");
        socketClient.exchange("SET d e");

        final SyncClient syncClient = new SyncClient(host, port);
        assertTrue(syncClient.connect());
        final CompletableFuture<Object> res = syncClient.sendSync("localhost", PortUtil.getAvailablePort(),
                                                                  masterId, 1);
        final String[] respContent = res.get().toString().split(" ");
        if ("P".equals(respContent[0])) {
            assertEquals(4, respContent.length);
            assertEquals(masterId, respContent[1]);
            assertEquals("4", respContent[2]);
            final String commands = new String(Base64.getDecoder().decode(respContent[3]));
            assertEquals("SET b c\nSET c d\nSET d e", commands);
        } else {
            fail(Arrays.toString(respContent));
        }
    }
}
