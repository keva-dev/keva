package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
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

    @Test
    void multiClientGet() {
        try {
            val setAbc = client.exchange("set abc 123");
            assertEquals("1", setAbc);
        } catch (Exception e) {
            fail(e);
        }

        final List<Callable<Object>> tasks = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final int taskNum = 2;
        for (int i = 0; i < taskNum; i++) {
            final int finalI = i;
            tasks.add(() ->
                      {
                          System.out.println("task: " + finalI);
                          return client.exchange("get abc");
                      });
        }
        try {
            final List<Future<Object>> futures = executor.invokeAll(tasks, 2, TimeUnit.SECONDS);
            assertFalse(futures.isEmpty());
            assertEquals(taskNum, futures.size());
            for (Future<Object> future : futures) {
                assertEquals("123", future.get().toString());
            }

            System.out.println(futures.size());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(5)
    void getSetLongString() {
        try {
            final int aKB = 1024;
            String testStr = "a".repeat(Math.max(0, aKB));
            String setAbc = client.exchange("set abc " + testStr);
            String getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals(testStr, getAbc);

            final int aMB = 1024 * aKB;
            testStr = "a".repeat(Math.max(0, aMB));
            setAbc = client.exchange("set abc " + testStr);
            getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals(testStr, getAbc);

            final int MB4 = 4 * aMB;
            testStr = "a".repeat(Math.max(0, MB4));
            setAbc = client.exchange("set abc " + testStr);
            getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals(testStr, getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(20)
    void getSetLongerString() {
        try {
            final int aMB = 1024 * 1024;
            final int MB8 = 8 * aMB;
            String testStr = "a".repeat(Math.max(0, MB8));
            String setAbc = client.exchange("set abc " + testStr);
            String getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals(testStr, getAbc);

            final int MB16 = 16 * aMB;
            testStr = "a".repeat(Math.max(0, MB16));
            setAbc = client.exchange("set abc " + testStr);
            getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals(testStr, getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(5)
    void getSetLongKeyString() {
        try {
            final int aKB = 1023;
            String testStr = "a".repeat(Math.max(0, aKB));
            String setAbc = client.exchange("set " + testStr + " 123");
            String getAbc = client.exchange("get " + testStr);
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }
}
