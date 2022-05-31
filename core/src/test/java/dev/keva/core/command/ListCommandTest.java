package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListCommandTest extends BaseCommandTest {

    @Test
    void wrongTypeSet() {
        val setAbc = jedis.set("abc", "123");
        assertEquals("OK", setAbc);
        assertThrows(JedisDataException.class, () -> jedis.lpush("abc", "123"));
    }

    @Test
    void wrongTypeSet2() {
        val setAbc = jedis.lpush("abc", "123");
        assertEquals(1, setAbc);
        assertThrows(JedisDataException.class, () -> jedis.hset("abc", "key", "val"));
    }

    @Test
    void wrongTypeSet3() {
        val setAbc = jedis.hset("abc", "key", "val");
        assertEquals(1, setAbc);
        assertThrows(JedisDataException.class, () -> jedis.lpush("abc", "123"));
    }

    @Test
    void lpush() {
        val lpush = jedis.lpush("test", "val");
        assertEquals(1, lpush);
    }

    @Test
    void rpush() {
        val rpush = jedis.rpush("test", "val");
        assertEquals(1, rpush);
    }

    @Test
    void lpop() {
        val lpush = jedis.lpush("test", "val");
        val lpop = jedis.lpop("test");
        assertEquals(1, lpush);
        assertEquals("val", lpop);
    }

    @Test
    void rpop() {
        val lpush = jedis.lpush("test", "val");
        val rpop = jedis.rpop("test");
        assertEquals(1, lpush);
        assertEquals("val", rpop);
    }

    @Test
    void llen() {
        val lpush = jedis.lpush("test", "val");
        val llen = jedis.llen("test");
        assertEquals(1, lpush);
        assertEquals(1, llen);
    }

    @Test
    void lrange() {
        val lpush = jedis.lpush("test", "val");
        val lrange = jedis.lrange("test", 0, 1);
        assertEquals(1, lpush);
        assertEquals(1, lrange.size());
        assertEquals("val", lrange.get(0));
    }

    @Test
    void lindex() {
        val lpush = jedis.lpush("test", "val");
        val lindex = jedis.lindex("test", 0);
        assertEquals(1, lpush);
        assertEquals("val", lindex);
    }

    @Test
    void lset() {
        val lpush = jedis.lpush("test", "val");
        val lset = jedis.lset("test", 0, "newVal");
        assertEquals(1, lpush);
        assertEquals("OK", lset);
    }

    @Test
    void lrem() {
        val lpush = jedis.lpush("test", "val");
        val lrem = jedis.lrem("test", 0, "val");
        assertEquals(1, lpush);
        assertEquals(1, lrem);
    }

}
