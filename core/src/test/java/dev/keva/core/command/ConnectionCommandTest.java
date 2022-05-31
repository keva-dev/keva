package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionCommandTest extends BaseCommandTest {

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
    void clientInfo() {
        try {
            val info = jedis.clientInfo();
            assertNotNull(info);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void clientId() {
        try {
            val info = jedis.clientId();
            assertNotNull(info);
        } catch (Exception e) {
            fail(e);
        }
    }
}
