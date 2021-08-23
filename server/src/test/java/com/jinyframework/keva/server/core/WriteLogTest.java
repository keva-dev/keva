package com.jinyframework.keva.server.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class WriteLogTest {

    private static final String[] EMPTY_ARRAY = new String[0];

    @Test
    void checkCurrentAndStartingOffset() {
        final WriteLog writeLog = new WriteLog(3);
        assertEquals(0, writeLog.getCurrentOffset());
        assertEquals(0, writeLog.getStartingOffset());

        writeLog.buffer("A");
        writeLog.buffer("B");
        writeLog.buffer("C");

        assertEquals(3, writeLog.getCurrentOffset());
        assertEquals(0, writeLog.getStartingOffset());

        writeLog.buffer("D");
        assertEquals(3, writeLog.getStartingOffset());

        writeLog.buffer("E");
        writeLog.buffer("F");

        assertEquals(6, writeLog.getCurrentOffset());
        writeLog.buffer("G");
        assertEquals(6, writeLog.getStartingOffset());
        assertEquals(7, writeLog.getCurrentOffset());
    }

    @Test
    void checkCircularBuffer() throws Exception {
        final WriteLog writeLog = new WriteLog(3);
        assertArrayEquals(EMPTY_ARRAY, writeLog.copyFromOffset(0).toArray());
        writeLog.buffer("A");
        writeLog.buffer("B");
        writeLog.buffer("C");
        assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
        writeLog.buffer("D");
        assertArrayEquals(new String[]{"D"}, writeLog.copyFromOffset(3).toArray());
        assertArrayEquals(new String[]{"B", "C", "D"}, writeLog.copyFromOffset(1).toArray());
        writeLog.buffer("E");
        log.info("{}", writeLog.copyFromOffset(2));
        assertArrayEquals(new String[]{"C", "D", "E"}, writeLog.copyFromOffset(2).toArray());
    }

    @Test
    void whenOffsetUnavailable_throwIllegalArgumentException() {
        final WriteLog writeLog = new WriteLog(3);
        assertThrows(IllegalArgumentException.class, () -> {
            writeLog.copyFromOffset(-1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            writeLog.copyFromOffset(3);
        });
        writeLog.buffer("A");
        writeLog.buffer("B");
        writeLog.buffer("C");
        writeLog.buffer("D");
        assertThrows(IllegalArgumentException.class, () -> {
            writeLog.copyFromOffset(0);
        });
        writeLog.buffer("E");
        assertThrows(IllegalArgumentException.class, () -> {
            writeLog.copyFromOffset(1);
        });
    }

    @Test
    void whenReset_AllOffsetIsZero() {
        final WriteLog writeLog = new WriteLog(3);
        writeLog.buffer("A");
        writeLog.buffer("B");
        writeLog.buffer("C");
        writeLog.buffer("E");
        writeLog.reset();
        assertEquals(0, writeLog.getStartingOffset());
        assertEquals(0, writeLog.getCurrentOffset());
    }
}
