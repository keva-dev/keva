package dev.keva.store.lock;

import java.util.concurrent.locks.ReentrantLock;

public class SpinLock extends ReentrantLock {
    public SpinLock() {
        super(true);
    }

    public void lock() {
        while (!tryLock()) {
        }
    }

    public void unlock() {
        super.unlock();
    }
}
