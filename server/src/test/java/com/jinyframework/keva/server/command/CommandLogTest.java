package com.jinyframework.keva.server.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CommandLogTest {

    private static final String[] EMPTY_ARRAY = new String[0];

    @Test
    void checkCurrentAndStartingOffset() {
        final CommandLog commandLog = new CommandLog(3);
        assertEquals(0, commandLog.getCurrentOffset());
        assertEquals(0, commandLog.getStartingOffset());

        commandLog.buffer("A");
        commandLog.buffer("B");
        commandLog.buffer("C");

        assertEquals(3, commandLog.getCurrentOffset());
        assertEquals(0, commandLog.getStartingOffset());

        commandLog.buffer("D");
        assertEquals(3, commandLog.getStartingOffset());

        commandLog.buffer("E");
        commandLog.buffer("F");

        assertEquals(6, commandLog.getCurrentOffset());
        commandLog.buffer("G");
        assertEquals(6, commandLog.getStartingOffset());
        assertEquals(7, commandLog.getCurrentOffset());
    }

    @Test
    void checkCircularBuffer() throws Exception {
        final CommandLog commandLog = new CommandLog(3);
        assertArrayEquals(EMPTY_ARRAY, commandLog.copyFromOffset(0).toArray());
        commandLog.buffer("A");
        commandLog.buffer("B");
        commandLog.buffer("C");
        assertArrayEquals(new String[]{"A", "B", "C"}, commandLog.copyFromOffset(0).toArray());
        commandLog.buffer("D");
        assertArrayEquals(new String[]{"D"}, commandLog.copyFromOffset(3).toArray());
        assertArrayEquals(new String[]{"B", "C", "D"}, commandLog.copyFromOffset(1).toArray());
        commandLog.buffer("E");
        log.info("{}", commandLog.copyFromOffset(2));
        assertArrayEquals(new String[]{"C", "D", "E"}, commandLog.copyFromOffset(2).toArray());
    }

    @Test
    void whenOffsetUnavailable_throwIllegalArgumentException() {
        final CommandLog commandLog = new CommandLog(3);
        assertThrows(IllegalArgumentException.class, () -> {
            commandLog.copyFromOffset(-1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            commandLog.copyFromOffset(3);
        });
        commandLog.buffer("A");
        commandLog.buffer("B");
        commandLog.buffer("C");
        commandLog.buffer("D");
        assertThrows(IllegalArgumentException.class, () -> {
            commandLog.copyFromOffset(0);
        });
        commandLog.buffer("E");
        assertThrows(IllegalArgumentException.class, () -> {
            commandLog.copyFromOffset(1);
        });
    }

    @Test
    void whenReset_AllOffsetIsZero() {
        final CommandLog commandLog = new CommandLog(3);
        commandLog.buffer("A");
        commandLog.buffer("B");
        commandLog.buffer("C");
        commandLog.buffer("E");
        commandLog.reset();
        assertEquals(0, commandLog.getStartingOffset());
        assertEquals(0, commandLog.getCurrentOffset());
    }
}
