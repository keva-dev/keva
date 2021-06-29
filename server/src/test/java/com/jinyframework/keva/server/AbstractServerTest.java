package com.jinyframework.keva.server;

import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractServerTest {
    static SocketClient client;
    static IServer server;

    @Test
    void ping() {
        try {
            val pong = client.exchange("ping");
            assertTrue("PONG".contentEquals(pong));
            assertEquals("PONG", pong);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void info() {
        try {
            val info = client.exchange("info");
            assertFalse("null".contentEquals(info));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetNull() {
        try {
            val getNull = client.exchange("get anotherkey");
            assertEquals("null", getNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSet() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void del() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            val delAbc = client.exchange("del abc");
            val getAbcNull = client.exchange("get abc");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
            assertEquals("1", delAbc);
            assertEquals("null", getAbcNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetExpire() {
        try {
            val setAbc = client.exchange("set abc 123");
            val getAbc = client.exchange("get abc");
            val expireAbc = client.exchange("expire abc 1000");
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
            assertEquals("1", expireAbc);
            Thread.sleep(1500);
            val getAbcNull = client.exchange("get abc");
            assertEquals("null", getAbcNull);
        } catch (Exception e) {
            fail(e);
        }
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
            final int aKB = 1026;
            final String testStr = "a".repeat(Math.max(0, aKB));
            final String setAbc = client.exchange("set " + testStr + " 123");
            final String getAbc = client.exchange("get " + testStr);
            assertEquals("1", setAbc);
            assertEquals("123", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void fsync() {
        try {
            final Object fileContent = client.exchange("fsync");
            System.out.println("file content: " + fileContent);
        } catch (Exception e) {
            fail(e);
        }
    }
}
