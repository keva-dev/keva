package com.jinyframework.keva.server.core;

import java.time.Duration;

public interface SnapshotService {
    void start(Duration interval, String fileDir);

    void start(Duration interval);

    void recover(String fileDir);
}
