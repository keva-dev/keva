package dev.keva.server.core;

public interface Server extends Runnable {
    void run();

    void shutdown();
}
