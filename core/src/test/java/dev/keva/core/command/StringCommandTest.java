package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StringCommandTest extends BaseCommandTest {

    @Test
    void getSetNull() {
        val getNull = jedis.get("anotherkey");
        assertNull(getNull);
    }

    @Test
    void getSet() {
        val setAbc = jedis.set("abc", "123");
        val getAbc = jedis.get("abc");
        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
    }

    @Test
    void getSetEmptyString() {
        val setEmpty = jedis.set("empty", "");
        val getEmpty = jedis.get("empty");
        assertEquals("OK", setEmpty);
        assertEquals("", getEmpty);
    }

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
    @Timeout(5)
    void getSetLongString() {
        final int aKB = 1024;
        String testStr = String.join("", Collections.nCopies(aKB, "a"));
        String setAbc = jedis.set("abc", testStr);
        String getAbc = jedis.get("abc");
        assertEquals("OK", setAbc);
        assertEquals(testStr, getAbc);
    }

    @Test
    @Timeout(5)
    void getSetLongKeyString() {
        final int aKB = 1026;
        final String testStr = String.join("", Collections.nCopies(aKB, "a"));
        final String setAbc = jedis.set(testStr, "123");
        final String getAbc = jedis.get(testStr);
        assertEquals("OK", setAbc);
        assertEquals("123", getAbc);
    }

    @Test
    void getdel() {
        val setAbc = jedis.set("abc", "123");
        val getdelAbc = jedis.getDel("abc");
        val getAbcAgain = jedis.get("abc");
        assertEquals("OK", setAbc);
        assertEquals("123", getdelAbc);
        assertNull(getAbcAgain);
    }

    @Test
    void append() {
        val append1 = jedis.append("1", "Hello");
        val append2 = jedis.append("1", " World");
        val get = jedis.get("1");
        assertEquals(5, append1);
        assertEquals(11, append2);
        assertEquals("Hello World", get);
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
        val hset = jedis.hset("test", "key", "val");
        val hget = jedis.hget("test", "key");
        assertEquals(1, hset);
        assertEquals("val", hget);
    }

    @Test
    void strlen() {
        val set1 = jedis.set("mykey", "Hello World");
        assertEquals("OK", set1);
        Long strlen1 = jedis.strlen("mykey");
        Long strlen2 = jedis.strlen("nonexisting");
        assertEquals(11, strlen1);
        assertEquals(0, strlen2);
    }

    @Test
    void getset() {
        val set1 = jedis.set("mykey", "Hello");
        assertEquals("OK", set1);
        val getset1 = jedis.getSet("mykey", "World");
        assertEquals("Hello", getset1);
        val get1 = jedis.get("mykey");
        assertEquals("World", get1);
    }

    @Test
    void mget() {
        val set1 = jedis.set("mykey1", "Hello");
        assertEquals("OK", set1);
        val set2 = jedis.set("mykey2", "World");
        assertEquals("OK", set2);
        val mget1 = jedis.mget("mykey1", "mykey2", "nonexistingkey");
        assertEquals(mget1.size(), 3);
        assertEquals(mget1, Arrays.asList("Hello", "World", null));
    }

    @Test
    void setrange() {
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

    }

    @Test
    void decr() {
        String set1 = jedis.set("mykey", "10");
        assertEquals(set1, "OK");
        Long decr1 = jedis.decr("mykey");
        assertEquals(decr1, 9);
        String set2 = jedis.set("errKey", "foobar");
        assertThrows(JedisDataException.class, () -> jedis.decr("errKey"));

    }

    @Test
    void decrBy() {
        String set1 = jedis.set("mykey", "10");
        assertEquals(set1, "OK");
        Long decrby1 = jedis.decrBy("mykey", 5);
        assertEquals(decrby1, 5);
        Long decrby2 = jedis.decrBy("mykey", 10);
        assertEquals(decrby2, -5);
        String set2 = jedis.set("mykey2", "abc123");

        assertThrows(JedisDataException.class, () -> jedis.decrBy("mykey2", 10));
    }

    @Test
    void getRange() {
        String set1 = jedis.set("mykey", "This is a string");
        String getrange1 = jedis.getrange("mykey", 0, 3);
        assertEquals("This", getrange1);
        String getrange2 = jedis.getrange("mykey", -3, -1);
        assertEquals("ing", getrange2);
        String getrange3 = jedis.getrange("mykey", 0, -1);
        assertEquals("This is a string", getrange3);
        String getrange4 = jedis.getrange("mykey", 10, 100);
        assertEquals("string", getrange4);
        String getrange5 = jedis.getrange("mykey", 10, 5);
        assertEquals("", getrange5);
        String getrange6 = jedis.getrange("mykey", -10, 10);
        assertEquals("s a s", getrange6);
    }

    @Test
    void incrByFloat() {
        String set1 = jedis.set("mykey", "10.50");
        assertEquals(set1, "OK");
        Double incrbyfloat1 = jedis.incrByFloat("mykey", 0.1);
        assertEquals(incrbyfloat1, 10.6);
        Double incrbyfloat2 = jedis.incrByFloat("mykey", -5);
        assertEquals(incrbyfloat2, 5.6);
        String set2 = jedis.set("mykey", "5.0e3");
        assertEquals(set2, "OK");
        Double incrbyfloat3 = jedis.incrByFloat("mykey", 2.0e2);
        assertEquals(incrbyfloat3, 5200);
        String set3 = jedis.set("mykey3", "abc");
        assertEquals(set3, "OK");
        assertThrows(JedisDataException.class, () -> jedis.incrByFloat("mykey3", 123));
    }

    @Test
    void mset() {
        String mset1 = jedis.mset("key1", "Hello", "key2", "World");
        assertEquals("OK", mset1);
        String get1 = jedis.get("key1");
        assertEquals("Hello", get1);
        String get2 = jedis.get("key2");
        assertEquals("World", get2);
    }

    @Test
    void lcs1() {
        String mset1 = jedis.mset("key1", "ohmytext", "key2", "mynewtext");
        assertEquals("OK", mset1);

        LCSMatchResult lcs1 = jedis.strAlgoLCSKeys("key1", "key2", StrAlgoLCSParams.StrAlgoLCSParams().idx().withMatchLen());

        // match string: mytext
        assertEquals(lcs1.getLen(), 6);

        assertEquals(lcs1.getMatches().get(0).getA().getStart(), 4);
        assertEquals(lcs1.getMatches().get(0).getA().getEnd(), 7);
        assertEquals(lcs1.getMatches().get(0).getB().getStart(), 5);
        assertEquals(lcs1.getMatches().get(0).getB().getEnd(), 8);
        assertEquals(lcs1.getMatches().get(0).getMatchLen(), 4);

        assertEquals(lcs1.getMatches().get(1).getA().getStart(), 2);
        assertEquals(lcs1.getMatches().get(1).getA().getEnd(), 3);
        assertEquals(lcs1.getMatches().get(1).getB().getStart(), 0);
        assertEquals(lcs1.getMatches().get(1).getB().getEnd(), 1);
        assertEquals(lcs1.getMatches().get(1).getMatchLen(), 2);
    }

    @Test
    void lcs2() {
        String mset1 = jedis.mset("key1", "thisisasuperlongrandomtext", "key2", "isthisarandomlongtext");
        assertEquals("OK", mset1);

        LCSMatchResult lcs1 = jedis.strAlgoLCSKeys("key1", "key2", StrAlgoLCSParams.StrAlgoLCSParams().idx().withMatchLen());

        assertEquals(lcs1.getLen(), 15);

        assertEquals(lcs1.getMatches().get(0).getA().getStart(), 22);
        assertEquals(lcs1.getMatches().get(0).getA().getEnd(), 25);
        assertEquals(lcs1.getMatches().get(0).getB().getStart(), 17);
        assertEquals(lcs1.getMatches().get(0).getB().getEnd(), 20);
        assertEquals(lcs1.getMatches().get(0).getMatchLen(), 4);

        assertEquals(lcs1.getMatches().get(1).getA().getStart(), 16);
        assertEquals(lcs1.getMatches().get(1).getA().getEnd(), 21);
        assertEquals(lcs1.getMatches().get(1).getB().getStart(), 7);
        assertEquals(lcs1.getMatches().get(1).getB().getEnd(), 12);
        assertEquals(lcs1.getMatches().get(1).getMatchLen(), 6);

        assertEquals(lcs1.getMatches().get(2).getA().getStart(), 4);
        assertEquals(lcs1.getMatches().get(2).getA().getEnd(), 6);
        assertEquals(lcs1.getMatches().get(2).getB().getStart(), 4);
        assertEquals(lcs1.getMatches().get(2).getB().getEnd(), 6);
        assertEquals(lcs1.getMatches().get(2).getMatchLen(), 3);

        assertEquals(lcs1.getMatches().get(3).getA().getStart(), 2);
        assertEquals(lcs1.getMatches().get(3).getA().getEnd(), 3);
        assertEquals(lcs1.getMatches().get(3).getB().getStart(), 0);
        assertEquals(lcs1.getMatches().get(3).getB().getEnd(), 1);
        assertEquals(lcs1.getMatches().get(3).getMatchLen(), 2);
    }

    @Test
    void substr() {
        String set = jedis.set("mykey", "This is a string");
        assertEquals("OK", set);

        String substr1 = jedis.substr("mykey", 0, 3);
        assertEquals("This", substr1);

        String substr2 = jedis.substr("mykey", -3, -1);
        assertEquals("ing", substr2);

        String substr3 = jedis.substr("mykey", -3, -2);
        assertEquals("in", substr3);

        String substr4 = jedis.substr("mykey", -3, -3);
        assertEquals("i", substr4);

        String substr5 = jedis.substr("mykey", 0, -1);
        assertEquals("This is a string", substr5);

        String substr6 = jedis.substr("mykey", 0, -2);
        assertEquals("This is a strin", substr6);

        String substr7 = jedis.substr("mykey", 10, 100);
        assertEquals("string", substr7);
    }

    @Test
    void getex() {
        jedis.set("mykey", "Hello");
//            String getex = jedis.getEx("mykey", GetExParams.getExParams()); // How to call getex without param??
//            assertEquals("Hello", getex);
//            long ttl = jedis.ttl("mykey");
//            assertEquals(-1, ttl); // ttl unimplemented
//            String getex = jedis.getEx("mykey", GetExParams.getExParams().ex(60));
//            long ttl = jedis.ttl("mykey");
//            assert(ttl <= 60);
    }

    @Test
    void msetnx() {
        long val1 = jedis.msetnx("key1", "Hello", "key2", "there");
        assertEquals(1, val1);
        long val2 = jedis.msetnx("key2", "new", "key3", "world");
        assertEquals(0, val2);
        List<String> values = jedis.mget("key1", "key2", "key3");
        assertEquals("Hello", values.get(0));
        assertEquals("there", values.get(1));
        assertEquals(null, values.get(2));
    }

    @Test
    void psetnx() {
        String val1 = jedis.psetex("mykey", 1000, "Hello");
        assertEquals("OK", val1);
//            long val2 = jedis.pttl("mykey"); // pttl unimplemented
//            assert(val2 <= 1000);
    }

    @Test
    void setex() {
        String val1 = jedis.setex("mykey", 10, "Hello");
        assertEquals("OK", val1);
//            long val2 = jedis.pttl("mykey"); // pttl unimplemented
//            assert(val2 <= 1000);
    }

    @Test
    void setnx() {
        long val1 = jedis.setnx("mykey", "Hello");
        assertEquals(1, val1);
        long val2 = jedis.setnx("mykey", "World");
        assertEquals(0, val2);
        String val3 = jedis.get("mykey");
        assertEquals("Hello", val3);
    }

}
