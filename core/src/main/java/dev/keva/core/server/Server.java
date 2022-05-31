package dev.keva.core.server;

import java.util.concurrent.CompletableFuture;

public interface Server extends Runnable {
    CompletableFuture<Void> getReady();

    void run();

    void shutdown();

    void clear();
}
