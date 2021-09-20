package com.jinyframework.keva.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import server.IServer;
import util.SocketClient;

@Slf4j
public abstract class AbstractProxyTest {
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
            log.info("Start get null test");
            val getNull = client.exchange("get anotherkey");
            log.info("Got result");
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
}
