package com.jinyframework.keva.server;

import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.val;
import org.junit.jupiter.api.Test;

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
}
