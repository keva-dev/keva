package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ZAddParams;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ZSetCommandTest extends BaseCommandTest {

    @Test
    void zaddWithXXAndNXErrs() {
        assertThrows(JedisDataException.class, () ->
                jedis.zadd("zset", 1.0, "val", new ZAddParams().xx().nx()));
    }

    @Test
    void zaddSingleWithNxAndGtErrs() {
        assertThrows(JedisDataException.class, () ->
                jedis.zadd("zset", 1.0, "val", new ZAddParams().gt().nx()));
    }

    @Test
    void zaddSingleWithNxAndLtErrs() {
        assertThrows(JedisDataException.class, () ->
                jedis.zadd("zset", 1.0, "val", new ZAddParams().lt().nx()));
    }

    @Test
    void zaddSingleWithGtAndLtErrs() {
        assertThrows(JedisDataException.class, () ->
                jedis.zadd("zset", 1.0, "val", new ZAddParams().lt().gt()));
    }

    @Test
    void zaddSingleWithoutOptions() {
        Long result = jedis.zadd("zset", 1.0, "val");
        assertEquals(1, result);

        result = jedis.zadd("zset", 1.0, "val");
        assertEquals(0, result);
    }

    @Test
    void zaddMultipleWithoutOptions() {
        Map<String, Double> members = new HashMap<>();
        int numMembers = 100;
        for (int i = 0; i < numMembers; ++i) {
            members.put(Integer.toString(i), (double) i);
        }
        Long result = jedis.zadd("zset", members);
        assertEquals(numMembers, result);

        result = jedis.zadd("zset", members);
        assertEquals(0, result);
    }

    @Test
    void zaddCh() {
        Long result = jedis.zadd("zset", 1.0, "mem", new ZAddParams().ch());
        assertEquals(1, result);

        result = jedis.zadd("zset", 1.0, "mem", new ZAddParams().ch());
        assertEquals(0, result);

        result = jedis.zadd("zset", 2.0, "mem", new ZAddParams().ch());
        assertEquals(1, result);
    }

    @Test
    void zscoreNonExistingKey() {
        val result = jedis.zscore("key", "mem");
        assertNull(result);
    }

    @Test
    void zscoreNonExistingMember() {
        jedis.zadd("zset", 1.0, "mem");
        val result = jedis.zscore("zset", "foo");
        assertNull(result);
    }

    @Test
    void zscoreExistingMember() {
        jedis.zadd("zset", 1.0, "mem");
        val result = jedis.zscore("zset", "mem");
        assertEquals(result, 1.0);
    }

}
