package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SetCommandTest extends BaseCommandTest {

    @Test
    void sadd() {
        try {
            val sadd = jedis.sadd("test", "val");
            assertEquals(1, sadd);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void smembers() {
        try {
            val sadd = jedis.sadd("test", "val");
            val smembers = jedis.smembers("test");
            assertEquals(1, sadd);
            assertEquals(1, smembers.size());
            assertEquals("val", smembers.toArray()[0]);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void sismember() {
        try {
            val sadd = jedis.sadd("test", "val");
            val sismember = jedis.sismember("test", "val");
            assertEquals(1, sadd);
            assertEquals(true, sismember);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void scard() {
        try {
            val sadd = jedis.sadd("test", "val");
            val scard = jedis.scard("test");
            assertEquals(1, sadd);
            assertEquals(1, scard);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void sdiff() {
        try {
            val sadd = jedis.sadd("test", "val");
            val sdiff = jedis.sdiff("test", "test2");
            assertEquals(1, sadd);
            assertEquals(1, sdiff.size());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void sinter() {
        try {
            val sadd = jedis.sadd("test", "val");
            val sadd2 = jedis.sadd("test2", "val");
            val sinter = jedis.sinter("test", "test2");
            assertEquals(1, sadd);
            assertEquals(1, sadd2);
            assertEquals(1, sinter.size());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void sunion() {
        try {
            val sadd = jedis.sadd("test", "val");
            val sunion = jedis.sunion("test", "test2");
            assertEquals(1, sadd);
            assertEquals(1, sunion.size());
            assertEquals("val", sunion.toArray()[0]);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void smove() {
        try {
            val sadd = jedis.sadd("test", "val");
            val smove = jedis.smove("test", "test2", "val");
            assertEquals(1, sadd);
            assertEquals(1, smove);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void srem() {
        try {
            val sadd = jedis.sadd("test", "val");
            val srem = jedis.srem("test", "val");
            assertEquals(1, sadd);
            assertEquals(1, srem);
        } catch (Exception e) {
            fail(e);
        }
    }

}
