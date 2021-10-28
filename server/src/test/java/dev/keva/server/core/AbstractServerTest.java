package dev.keva.server.core;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractServerTest {
    static Jedis jedis;
    static Server server;

    @Test
    void ping() {
        try {
            val pong = jedis.ping();
            assertTrue("PONG".contentEquals(pong));
            assertEquals("PONG", pong);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void info() {
        try {
            val info = jedis.info();
            assertNotNull(info);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetNull() {
        try {
            val getNull = jedis.get("anotherkey");
            assertNull(getNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSet() {
        try {
            val setAbc = jedis.set("abc", "123");
            val getAbc = jedis.get("abc");
            assertEquals("OK", setAbc);
            assertEquals("123", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void del() {
        try {
            var setAbc = jedis.set("abc", "123");
            val getAbc = jedis.get("abc");
            val delAbc = jedis.del("abc");
            val getAbcNull = jedis.get("abc");
            assertEquals("OK", setAbc);
            assertEquals("123", getAbc);
            assertEquals(1, delAbc);
            assertNull(getAbcNull);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetExpire() {
        try {
            val setAbc = jedis.set("abc", "123");
            val getAbc = jedis.get("abc");
            val expireAbc = jedis.expire("abc", 1000);

            assertEquals("OK", setAbc);
            assertEquals("123", getAbc);
            assertEquals(1, expireAbc);
            Thread.sleep(1500);
            val getAbcNull = jedis.get("abc");
            assertNull(getAbcNull);
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
            String setAbc = jedis.set("abc", testStr);
            String getAbc = jedis.get("abc");
            assertEquals("OK", setAbc);
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
            final String setAbc = jedis.set(testStr, "123");
            final String getAbc = jedis.get(testStr);
            assertEquals("OK", setAbc);
            assertEquals("123", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }
}
