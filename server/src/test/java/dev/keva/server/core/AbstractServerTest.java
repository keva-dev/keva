package dev.keva.server.core;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractServerTest {
    static Jedis jedis;
    static Server server;
    static Jedis subscriber;

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
            val expireAbc = jedis.expire("abc", 1);

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

    @Test
    @Timeout(30)
    void pubsub() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Thread(() -> {
            subscriber.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    future.complete(message);
                }

                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    jedis.publish("test", "Test message");
                }
            }, "test");
        }).start();
        val message = future.get();
        assertEquals("Test message", message);
    }

    @Test
    void incr() {
        // with exist key
        String res = jedis.set("1to2", "1");
        assertEquals("OK", res);
        Long newVal = jedis.incr("1to2");
        assertEquals(2, newVal);
        String val = jedis.get("1to2");
        assertEquals("2", val);

        // with non exist key
        res = jedis.get("0to1");
        assertNull(res);
        newVal = jedis.incr("0to1");
        assertEquals(1, newVal);
        val = jedis.get("0to1");
        assertEquals("1", val);

        // with wrong key type
        res = jedis.set("wrong", "type");
        assertEquals("OK", res);
        assertThrows(JedisDataException.class, () -> jedis.incr("wrong"));
    }

    @Test
    void incrBy() {
        // with exist key
        String res = jedis.set("1to5", "1");
        assertEquals("OK", res);
        Long newVal = jedis.incrBy("1to5", 4);
        assertEquals(5, newVal);
        String val = jedis.get("1to5");
        assertEquals("5", val);

        // with non exist key
        res = jedis.get("0to10");
        assertNull(res);
        newVal = jedis.incrBy("0to10", 10);
        assertEquals(10, newVal);
        val = jedis.get("0to10");
        assertEquals("10", val);

        // with wrong key type
        res = jedis.set("wrong", "type");
        assertEquals("OK", res);
        assertThrows(JedisDataException.class, () -> jedis.incrBy("wrong", 10));
    }
}
