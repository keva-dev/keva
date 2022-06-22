package dev.keva.core.command;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

public class GenericCommandTest extends BaseCommandTest {

    @Test
    void del() {
        String setAbc = jedis.set("abc", "123");
        val getAbc = jedis.get("abc");
        val delAbc = jedis.del("abc");
        val getAbcNull = jedis.get("abc");
        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
        assertEquals(1, delAbc);
        assertNull(getAbcNull);
    }

    @Test
    void exists() {
        jedis.set("key1", "Hello");
        jedis.set("key2", "World");
        Boolean existsKey1 = jedis.exists("key1");
        Boolean existsNoSuchKey = jedis.exists("nosuchkey");
        Long existsKey1Key2 = jedis.exists("key1", "key2", "nosuchkey");
        assertFalse(existsNoSuchKey);
        assertTrue(existsKey1);
        assertEquals(2, existsKey1Key2);
    }

    @Test
    @SneakyThrows
    void getSetExpire() {
        val setAbc = jedis.set("abc", "123");
        String getAbc = jedis.get("abc");
        val expireAbc = jedis.expire("abc", 1L);

        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
        assertEquals(1, expireAbc);
        Thread.sleep(500);
        getAbc = jedis.get("abc");
        assertEquals("123", getAbc);
        Thread.sleep(501);
        val getAbcNull = jedis.get("abc");
        assertNull(getAbcNull);
    }

    @Test
    @SneakyThrows
    void updateExpire() {
        val setAbc = jedis.set("abc", "123");
        String getAbc = jedis.get("abc");
        Long expireAbc = jedis.expire("abc", 1L);

        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
        assertEquals(1, expireAbc);

        Thread.sleep(500);
        getAbc = jedis.get("abc");
        expireAbc = jedis.expire("abc", 1L);
        assertEquals(1, expireAbc);
        assertEquals("123", getAbc);

        Thread.sleep(501);
        getAbc = jedis.get("abc");
        assertEquals("123", getAbc);

        Thread.sleep(1001);
        val getAbcNull = jedis.get("abc");
        assertNull(getAbcNull);
    }

    @Test
    @SneakyThrows
    void getSetExpireAt() {
        val setAbc = jedis.set("abc", "123");
        String getAbc = jedis.get("abc");
        val oneSecondLaterTime = System.currentTimeMillis() + 1000;
        val expireAbc = jedis.expireAt("abc", oneSecondLaterTime);

        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
        assertEquals(1, expireAbc);
        Thread.sleep(500);
        getAbc = jedis.get("abc");
        assertEquals("123", getAbc);
        Thread.sleep(501);
        val getAbcNull = jedis.get("abc");
        assertNull(getAbcNull);
    }

    @Test
    @SneakyThrows
    void setAfterExpireAt() {
        String setAbc = jedis.set("abc", "123");
        String getAbc = jedis.get("abc");
        val oneSecondLaterTime = System.currentTimeMillis() + 1000;
        val expireAbc = jedis.expireAt("abc", oneSecondLaterTime);

        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
        assertEquals(1, expireAbc);
        Thread.sleep(500);
        getAbc = jedis.get("abc");
        assertEquals("123", getAbc);
        setAbc = jedis.set("abc", "456");
        assertEquals("OK", setAbc);
        Thread.sleep(501);
        getAbc = jedis.get("abc");
        assertEquals("456", getAbc);
    }

    @Test
    @Timeout(5)
    void rename() {
        final String initKey = "Key";
        final String initVal = "Val";
        final String newKey = "Nkey";
        final String renameBeforeSet = jedis.rename(initKey, newKey);
        jedis.set(initKey, initVal);
        final String renameAfterSet = jedis.rename(initKey, newKey);
        final String getAfterRename = jedis.get(newKey);
        assertEquals("ERR unknown key", renameBeforeSet);
        assertEquals("OK", renameAfterSet);
        assertEquals(initVal, getAfterRename);
    }

    @Test
    @Timeout(5)
    @SneakyThrows
    void renameWithExpire() {
        val initKey = "Key";
        val initVal = "Val";
        val newKey = "Nkey";
        val renameBeforeSet = jedis.rename(initKey, newKey);
        jedis.set(initKey, initVal);
        jedis.expireAt(initKey, System.currentTimeMillis() + 500);
        val renameAfterSet = jedis.rename(initKey, newKey);
        String getAfterRename = jedis.get(newKey);
        assertEquals("ERR unknown key", renameBeforeSet);
        assertEquals("OK", renameAfterSet);
        assertEquals(initVal, getAfterRename);
        Thread.sleep(501);
        getAfterRename = jedis.get(newKey);
        assertNull(getAfterRename);
    }

    @Test
    void dumpAndRestore() {
        val set1 = jedis.set("key1", "Hello World");
        assertEquals("OK", set1);
        val dump1 = jedis.dump("key1");
        assertNotNull(dump1);
        val restore1 = jedis.restore("key2", 0L, dump1);
        assertEquals("OK", restore1);
        val key2 = jedis.get("key2");
        assertEquals("Hello World", key2);
    }

    @Test
    void type() {
        val set1 = jedis.set("key1", "Hello World");
        assertEquals("OK", set1);
        val type1 = jedis.type("key1");
        assertEquals("string", type1);
        val type2 = jedis.type("key2");
        assertEquals("none", type2);
        val hashSet = jedis.hset("key3", "field1", "value1");
        assertEquals(1, hashSet.intValue());
        val type3 = jedis.type("key3");
        assertEquals("hash", type3);
    }

    @Test
    void keys() {
        val set1 = jedis.set("key1", "Hello World");
        assertEquals("OK", set1);
        val set2 = jedis.set("key2", "Hello World");
        assertEquals("OK", set2);
        val keys = jedis.keys("*");
        assertEquals(2, keys.size());
        assertEquals("key1", keys.toArray()[0]);
        assertEquals("key2", keys.toArray()[1]);
    }

}
