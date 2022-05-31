package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionCommandTest extends BaseCommandTest {

    @Test
    void ping() {
        val pong = jedis.ping();
        assertTrue("PONG".contentEquals(pong));
        assertEquals("PONG", pong);
    }

    @Test
    void clientInfo() {
        val info = jedis.clientInfo();
        assertNotNull(info);
    }

    @Test
    void clientId() {
        val info = jedis.clientId();
        assertNotNull(info);
    }
}
