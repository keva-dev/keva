package com.jinyframework.keva.server.core;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


class WriteLogMultiTest {
    @Test
    void readThenWrite() throws Throwable {
        TestFramework.runOnce(new R_W_R_Case());
    }

    @Test
    void multiReadOneWrite() throws Throwable {
        TestFramework.runManyTimes(new RR_W_RR_Case(), 100);
    }

    @Test
    void multiReadOneWrite_withOverwriting() throws Throwable {
        TestFramework.runManyTimes(new RR_W_RR_Overwrite_Case(), 100);
    }


    @SuppressWarnings("unused")
    static class R_W_R_Case extends MultithreadedTestCase {
        WriteLog writeLog;

        @Override
        public void initialize() {
            writeLog = new WriteLog(3);
            writeLog.buffer("A");
            writeLog.buffer("B");
        }

        public void thread1() {
            assertArrayEquals(new String[]{"A", "B"}, writeLog.copyFromOffset(0).toArray());
            waitForTick(2);
            assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
        }

        public void thread2() {
            waitForTick(1);
            writeLog.buffer("C");
        }
    }

    @SuppressWarnings("unused")
    static class RR_W_RR_Case extends MultithreadedTestCase {
        WriteLog writeLog;

        @Override
        public void initialize() {
            writeLog = new WriteLog(3);
            writeLog.buffer("A");
            writeLog.buffer("B");
        }

        public void thread1() {
            assertArrayEquals(new String[]{"A", "B"}, writeLog.copyFromOffset(0).toArray());
            waitForTick(2);
            assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
            Assertions.assertEquals(3, writeLog.getCurrentOffset());
        }

        public void thread2() {
            waitForTick(1);
            writeLog.buffer("C");
        }

        public void thread3() {
            assertArrayEquals(new String[]{"A", "B"}, writeLog.copyFromOffset(0).toArray());
            waitForTick(2);
            assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
            Assertions.assertEquals(3, writeLog.getCurrentOffset());
        }
    }

    @SuppressWarnings("unused")
    static class RR_W_RR_Overwrite_Case extends MultithreadedTestCase {
        WriteLog writeLog;

        @Override
        public void initialize() {
            writeLog = new WriteLog(3);
            writeLog.buffer("A");
            writeLog.buffer("B");
            writeLog.buffer("C");
        }

        public void thread1() {
            assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
            Assertions.assertEquals(3, writeLog.getCurrentOffset());
            Assertions.assertEquals(0, writeLog.getMinOffset());
            waitForTick(2);
            assertArrayEquals(new String[]{"B", "C", "D"}, writeLog.copyFromOffset(1).toArray());
            Assertions.assertEquals(4, writeLog.getCurrentOffset());
            Assertions.assertEquals(1, writeLog.getMinOffset());
        }

        public void thread2() {
            waitForTick(1);
            writeLog.buffer("D");
            Assertions.assertEquals(4, writeLog.getCurrentOffset());
            Assertions.assertEquals(1, writeLog.getMinOffset());
        }

        public void thread3() {
            assertArrayEquals(new String[]{"A", "B", "C"}, writeLog.copyFromOffset(0).toArray());
            Assertions.assertEquals(3, writeLog.getCurrentOffset());
            Assertions.assertEquals(0, writeLog.getMinOffset());
            waitForTick(2);
            assertArrayEquals(new String[]{"B", "C", "D"}, writeLog.copyFromOffset(1).toArray());
            Assertions.assertEquals(4, writeLog.getCurrentOffset());
            Assertions.assertEquals(1, writeLog.getMinOffset());
        }
    }
}
