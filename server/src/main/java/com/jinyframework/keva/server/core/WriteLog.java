package com.jinyframework.keva.server.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A circular array that supports multiple thread reads and single thread write
 */
@Slf4j
public class WriteLog {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);


    private final AtomicInteger currentOffset = new AtomicInteger(0); // increase every write

    private final AtomicInteger startingOffset = new AtomicInteger(0); // denotes offset at index 0

    private final int capacity;
    private final ArrayList<String> buffer;

    /**
     * Instantiates a new Command log.
     *
     * @param capacity the capacity
     */
    public WriteLog(int capacity) {
        this.capacity = capacity;
        buffer = new ArrayList<>(Arrays.asList(new String[capacity]));
    }

    /**
     * Buffer the command, if capacity limit is reached, it will wraps around and override data in the first index
     *
     * @param command the command
     */
    public void buffer(String command) {
        lock.writeLock().lock();
        try {
            final int offset = currentOffset.getAndIncrement();
            final int index = offset % capacity;
            if (offset >= startingOffset.get() + capacity) {
                startingOffset.set(offset);
            }
            buffer.set(index, command);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets current offset.
     *
     * @return the current offset
     */
    public int getCurrentOffset() {
        return currentOffset.get();
    }

    /**
     * Gets starting offset.
     *
     * @return the starting offset
     */
    public int getStartingOffset() {
        return startingOffset.get();
    }

    /**
     * Copy from provided offset to current offset array list.
     *
     * @param offset the offset
     * @return the array list
     * @throws IllegalArgumentException if offset is larger than current available or smaller than minimum available
     */
    public ArrayList<String> copyFromOffset(int offset) {
        lock.readLock().lock();
        try {
            final int cur = currentOffset.get();
            final int start = startingOffset.get();
            final int min = Math.max(cur - capacity, 0);
            if (offset > cur || offset < min) {
                throw new IllegalArgumentException("Offset not available");
            }
            final ArrayList<String> result = new ArrayList<>();
            if (offset < start) {
                result.addAll(buffer.subList(min % capacity, capacity));
                result.addAll(buffer.subList(0, cur - start));
            } else {
                result.addAll(buffer.subList(offset - start, cur - start));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Reset offset, clear all data.
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            currentOffset.set(0);
            startingOffset.set(0);
            buffer.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
