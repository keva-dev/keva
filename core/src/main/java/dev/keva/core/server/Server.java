package dev.keva.core.server;

public interface Server extends Runnable {
    void run();

    void await();

    void shutdown();

    void clear();
}
