package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetCommandTest extends BaseCommandTest {

    @Test
    void sadd() {
        val sadd = jedis.sadd("test", "val");
        assertEquals(1, sadd);
    }

    @Test
    void smembers() {
        val sadd = jedis.sadd("test", "val");
        val smembers = jedis.smembers("test");
        assertEquals(1, sadd);
        assertEquals(1, smembers.size());
        assertEquals("val", smembers.toArray()[0]);
    }

    @Test
    void sismember() {
        val sadd = jedis.sadd("test", "val");
        val sismember = jedis.sismember("test", "val");
        assertEquals(1, sadd);
        assertEquals(true, sismember);
    }

    @Test
    void scard() {
        val sadd = jedis.sadd("test", "val");
        val scard = jedis.scard("test");
        assertEquals(1, sadd);
        assertEquals(1, scard);
    }

    @Test
    void sdiff() {
        val sadd = jedis.sadd("test", "val");
        val sdiff = jedis.sdiff("test", "test2");
        assertEquals(1, sadd);
        assertEquals(1, sdiff.size());
    }

    @Test
    void sinter() {
        val sadd = jedis.sadd("test", "val");
        val sadd2 = jedis.sadd("test2", "val");
        val sinter = jedis.sinter("test", "test2");
        assertEquals(1, sadd);
        assertEquals(1, sadd2);
        assertEquals(1, sinter.size());
    }

    @Test
    void sunion() {
        val sadd = jedis.sadd("test", "val");
        val sunion = jedis.sunion("test", "test2");
        assertEquals(1, sadd);
        assertEquals(1, sunion.size());
        assertEquals("val", sunion.toArray()[0]);
    }

    @Test
    void smove() {
        val sadd = jedis.sadd("test", "val");
        val smove = jedis.smove("test", "test2", "val");
        assertEquals(1, sadd);
        assertEquals(1, smove);
    }

    @Test
    void srem() {
        val sadd = jedis.sadd("test", "val");
        val srem = jedis.srem("test", "val");
        assertEquals(1, sadd);
        assertEquals(1, srem);
    }

}
