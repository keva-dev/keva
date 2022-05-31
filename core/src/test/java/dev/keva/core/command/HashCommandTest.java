package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HashCommandTest extends BaseCommandTest {

    @Test
    void hsetGet() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hget = jedis.hget("test", "key");
            assertEquals(1, hset);
            assertEquals("val", hget);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hdel() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hdel = jedis.hdel("test", "key");
            assertEquals(1, hset);
            assertEquals(1, hdel);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hexists() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hexists = jedis.hexists("test", "key");
            assertEquals(1, hset);
            assertEquals(true, hexists);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hvals() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hvals = jedis.hvals("test");
            assertEquals(1, hset);
            assertEquals(1, hvals.size());
            assertEquals("val", hvals.get(0));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hkeys() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hkeys = jedis.hkeys("test");
            assertEquals(1, hset);
            assertEquals(1, hkeys.size());
            assertEquals("key", hkeys.toArray()[0]);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hlen() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hlen = jedis.hlen("test");
            assertEquals(1, hset);
            assertEquals(1, hlen);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hstrlen() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hstrlen = jedis.hstrlen("test", "key");
            assertEquals(1, hset);
            assertEquals(3, hstrlen);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void hgetAll() {
        try {
            val hset = jedis.hset("test", "key", "val");
            val hgetAll = jedis.hgetAll("test");
            assertEquals(1, hset);
            assertEquals(1, hgetAll.size());
            assertEquals("val", hgetAll.get("key"));
        } catch (Exception e) {
            fail(e);
        }
    }

}
