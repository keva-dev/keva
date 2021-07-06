package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.util.PortUtil;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SyncClientTest {
    static final String host = "localhost";
    static final int port = PortUtil.getAvailablePort();

    @BeforeAll
    @SneakyThrows
    static void startServer() {
        Files.createDirectories(Path.of("./temptest/"));

        final IServer server = new NettyServer(ConfigHolder.defaultBuilder()
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
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    void fullSync() {
        final SyncClient syncClient = new SyncClient(host, port);
        try {
            assertTrue(syncClient.connect());
            final Promise<Object> resPromise = syncClient.fullSync("localhost", PortUtil.getAvailablePort());
            resPromise.sync();
            assertTrue(resPromise.isSuccess());
            final byte[] res = (byte[]) resPromise.get();
            final byte[] actual = Files.readAllBytes(Path.of("./temptest/KevaData"));
            assertArrayEquals(actual, res);
        } catch (InterruptedException | ExecutionException | IOException e) {
            fail(e);
        }
    }
}
