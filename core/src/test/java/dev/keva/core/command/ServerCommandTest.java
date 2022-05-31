package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerCommandTest extends BaseCommandTest {

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

}
