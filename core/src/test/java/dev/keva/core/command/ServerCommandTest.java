package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerCommandTest extends BaseCommandTest {

    @Test
    void info() {
        val info = jedis.info();
        assertNotNull(info);
    }

    @Test
    void flush() {
        val setAbc = jedis.set("abc", "123");
        assertEquals("OK", setAbc);
        val flush = jedis.flushDB();
        assertEquals("OK", flush);
        val getAbc = jedis.get("abc");
        assertNull(getAbc);
    }

}
