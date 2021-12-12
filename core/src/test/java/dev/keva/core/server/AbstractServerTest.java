package dev.keva.core.server;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import lombok.var;

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
    void flush() {
        try {
            val setAbc = jedis.set("abc", "123");
            assertEquals("OK", setAbc);
            val flush = jedis.flushDB();
            assertEquals("OK", flush);
            val getAbc = jedis.get("abc");
            assertNull(getAbc);
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
    void getSetEmptyString() {
        try {
            val setEmpty = jedis.set("empty", "");
            val getEmpty = jedis.get("empty");
            assertEquals("OK", setEmpty);
            assertEquals("", getEmpty);
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
    void transaction() {
        try {
            val transaction = jedis.multi();
            transaction.set("abc", "123");
            transaction.set("def", "456");
            val exec = transaction.exec();
            assertNotNull(exec);
            assertEquals(2, exec.size());
            assertEquals("OK", exec.get(0));
            assertEquals("OK", exec.get(1));

            val getAbc = jedis.get("abc");
            val getDef = jedis.get("def");
            assertEquals("123", getAbc);
            assertEquals("456", getDef);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void transactionWatch() {
        try {
            jedis.watch("abc");
            val transaction = jedis.multi();
            transaction.set("abc", "123");
            transaction.set("abc", "456");
            val exec = transaction.exec();
            assertNotNull(exec);
            assertEquals(2, exec.size());
            assertEquals("OK", exec.get(0));
            assertEquals("OK", exec.get(1));

            val getAbc = jedis.get("abc");
            assertEquals("456", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetExpire() {
        try {
            val setAbc = jedis.set("abc", "123");
            var getAbc = jedis.get("abc");
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void updateExpire() {
        try {
            val setAbc = jedis.set("abc", "123");
            var getAbc = jedis.get("abc");
            var expireAbc = jedis.expire("abc", 1L);

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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSetExpireAt() {
        try {
            val setAbc = jedis.set("abc", "123");
            var getAbc = jedis.get("abc");
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void setAfterExpireAt() {
        try {
            var setAbc = jedis.set("abc", "123");
            var getAbc = jedis.get("abc");
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(5)
    void getSetLongString() {
        try {
            final int aKB = 1024;
            String testStr = String.join("", Collections.nCopies(aKB, "a"));
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
            final String testStr = String.join("", Collections.nCopies(aKB, "a"));
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
    @Timeout(5)
    void rename() {
        try {
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @Timeout(5)
    void renameWithExpire() {
        try {
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
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getdel() {
        try {
            val setAbc = jedis.set("abc", "123");
            val getdelAbc = jedis.getDel("abc");
            val getAbcAgain = jedis.get("abc");
            assertEquals("OK", setAbc);
            assertEquals("123", getdelAbc);
            assertNull(getAbcAgain);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void append() {
        try {
            val append1 = jedis.append("1", "Hello");
            val append2 = jedis.append("1", " World");
            val get = jedis.get("1");
            assertEquals(5, append1);
            assertEquals(11, append2);
            assertEquals("Hello World", get);
        } catch (Exception e) {
            fail(e);
        }
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

    @Test
    void strlen() {
        try {
            val set1 = jedis.set("mykey", "Hello World");
            assertEquals("OK", set1);
            Long strlen1 = jedis.strlen("mykey");
            Long strlen2 = jedis.strlen("nonexisting");
            assertEquals(11, strlen1);
            assertEquals(0, strlen2);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getset() {
        try {
            val set1 = jedis.set("mykey", "Hello");
            assertEquals("OK", set1);
            val getset1 = jedis.getSet("mykey", "World");
            assertEquals("Hello", getset1);
            val get1 = jedis.get("mykey");
            assertEquals("World", get1);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void mget() {
        try {
            val set1 = jedis.set("mykey1", "Hello");
            assertEquals("OK", set1);
            val set2 = jedis.set("mykey2", "World");
            assertEquals("OK", set2);
            val mget1 = jedis.mget("mykey1", "mykey2", "nonexistingkey");
            assertEquals(mget1.size(), 3);
            assertEquals(mget1, Arrays.asList("Hello", "World", null));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void setrange() {
        try {
            val set1 = jedis.set("key1", "Hello World");
            assertEquals("OK", set1);
            val setrange1 = jedis.setrange("key1", 6, "Keva");
            assertEquals(11, setrange1);
            val get1 = jedis.get("key1");
            assertEquals("Hello Kevad", get1);

            val setrange2 = jedis.setrange("key2", 5, "Kevas");
            assertEquals(10, setrange2);
            val get2 = jedis.get("key2");
            assertEquals("\u0000\u0000\u0000\u0000\u0000Kevas", get2);

            val setrange3 = jedis.setrange("key2", 0, "Ke");
            assertEquals(10, setrange3);
            val get3 = jedis.get("key2");
            assertEquals("Ke\u0000\u0000\u0000Kevas", get3);

        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void dumpAndRestore() {
        try {
            val set1 = jedis.set("key1", "Hello World");
            assertEquals("OK", set1);
            val dump1 = jedis.dump("key1");
            assertNotNull(dump1);
            val restore1 = jedis.restore("key2", 0, dump1);
            assertEquals("OK", restore1);
            val key2 = jedis.get("key2");
            assertEquals("Hello World", key2);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void type() {
        try {
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
        } catch (Exception e) {
            fail(e);
        }
    }
}
