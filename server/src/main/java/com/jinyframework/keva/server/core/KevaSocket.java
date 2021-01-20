package com.jinyframework.keva.server.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Getter
@Setter
public class KevaSocket {
    private final Socket socket;
    private final String id;
    private AtomicLong lastOnlineLong;
    private AtomicBoolean alive;

    public boolean isAlive() {
        return alive.get();
    }

    public long getLastOnline() {
        return lastOnlineLong.get();
    }
}
