package dev.keva.store.lock;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SpinLock extends ReentrantReadWriteLock {
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    public SpinLock() {
        super(true);
        readLock = readLock();
        writeLock = writeLock();
    }

    public void sharedLock() {
        while (!readLock.tryLock());
    }

    public void sharedUnlock() {
        readLock.unlock();
    }

    public void exclusiveLock() {
        while (!writeLock.tryLock());
    }

    public void exclusiveUnlock() {
        writeLock.unlock();
    }
}
