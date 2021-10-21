package dev.keva.server.core;

public interface Server extends Runnable {
    void run(boolean isFreshStart);

    void shutdown();
}
