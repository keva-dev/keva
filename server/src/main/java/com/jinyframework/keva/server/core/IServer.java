package com.jinyframework.keva.server.core;

public interface IServer extends Runnable {
    void run(boolean isFreshStart);

    void shutdown();
}
