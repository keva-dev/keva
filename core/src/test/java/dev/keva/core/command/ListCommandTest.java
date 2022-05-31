package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ListCommandTest extends BaseCommandTest {

    @Test
    void wrongTypeSet() {
        try {
            val setAbc = jedis.set("abc", "123");
            assertEquals("OK", setAbc);
            jedis.lpush("abc", "123");
        } catch (Exception e) {
            assertEquals(JedisDataException.class, e.getClass());
        }
    }

    @Test
    void wrongTypeSet2() {
        try {
            val setAbc = jedis.lpush("abc", "123");
            assertEquals(1, setAbc);
            jedis.hset("abc", "key", "val");
        } catch (Exception e) {
            assertEquals(JedisDataException.class, e.getClass());
        }
    }

    @Test
    void wrongTypeSet3() {
        try {
            val setAbc = jedis.hset("abc", "key", "val");
            assertEquals(1, setAbc);
            jedis.lpush("abc", "123");
        } catch (Exception e) {
            assertEquals(JedisDataException.class, e.getClass());
        }
    }

    @Test
    void lpush() {
        try {
            val lpush = jedis.lpush("test", "val");
            assertEquals(1, lpush);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void rpush() {
        try {
            val rpush = jedis.rpush("test", "val");
            assertEquals(1, rpush);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void lpop() {
        try {
            val lpush = jedis.lpush("test", "val");
            val lpop = jedis.lpop("test");
            assertEquals(1, lpush);
            assertEquals("val", lpop);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void rpop() {
        try {
            val lpush = jedis.lpush("test", "val");
            val rpop = jedis.rpop("test");
            assertEquals(1, lpush);
            assertEquals("val", rpop);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void llen() {
        try {
            val lpush = jedis.lpush("test", "val");
            val llen = jedis.llen("test");
            assertEquals(1, lpush);
            assertEquals(1, llen);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void lrange() {
        try {
            val lpush = jedis.lpush("test", "val");
            val lrange = jedis.lrange("test", 0, 1);
            assertEquals(1, lpush);
            assertEquals(1, lrange.size());
            assertEquals("val", lrange.get(0));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void lindex() {
        try {
            val lpush = jedis.lpush("test", "val");
            val lindex = jedis.lindex("test", 0);
            assertEquals(1, lpush);
            assertEquals("val", lindex);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void lset() {
        try {
            val lpush = jedis.lpush("test", "val");
            val lset = jedis.lset("test", 0, "newVal");
            assertEquals(1, lpush);
            assertEquals("OK", lset);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void lrem() {
        try {
            val lpush = jedis.lpush("test", "val");
            val lrem = jedis.lrem("test", 0, "val");
            assertEquals(1, lpush);
            assertEquals(1, lrem);
        } catch (Exception e) {
            fail(e);
        }
    }

}
