package dev.keva.core.server;

public interface Server extends Runnable {
    void run();

    void shutdown();

    void clear();
}
