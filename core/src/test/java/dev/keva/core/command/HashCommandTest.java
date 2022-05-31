package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashCommandTest extends BaseCommandTest {

    @Test
    void hsetGet() {
        val hset = jedis.hset("test", "key", "val");
        val hget = jedis.hget("test", "key");
        assertEquals(1, hset);
        assertEquals("val", hget);
    }

    @Test
    void hdel() {
        val hset = jedis.hset("test", "key", "val");
        val hdel = jedis.hdel("test", "key");
        assertEquals(1, hset);
        assertEquals(1, hdel);
    }

    @Test
    void hexists() {
        val hset = jedis.hset("test", "key", "val");
        val hexists = jedis.hexists("test", "key");
        assertEquals(1, hset);
        assertEquals(true, hexists);
    }

    @Test
    void hvals() {
        val hset = jedis.hset("test", "key", "val");
        val hvals = jedis.hvals("test");
        assertEquals(1, hset);
        assertEquals(1, hvals.size());
        assertEquals("val", hvals.get(0));
    }

    @Test
    void hkeys() {
        val hset = jedis.hset("test", "key", "val");
        val hkeys = jedis.hkeys("test");
        assertEquals(1, hset);
        assertEquals(1, hkeys.size());
        assertEquals("key", hkeys.toArray()[0]);
    }

    @Test
    void hlen() {
        val hset = jedis.hset("test", "key", "val");
        val hlen = jedis.hlen("test");
        assertEquals(1, hset);
        assertEquals(1, hlen);
    }

    @Test
    void hstrlen() {
        val hset = jedis.hset("test", "key", "val");
        val hstrlen = jedis.hstrlen("test", "key");
        assertEquals(1, hset);
        assertEquals(3, hstrlen);
    }

    @Test
    void hgetAll() {
        val hset = jedis.hset("test", "key", "val");
        val hgetAll = jedis.hgetAll("test");
        assertEquals(1, hset);
        assertEquals(1, hgetAll.size());
        assertEquals("val", hgetAll.get("key"));
    }

}
