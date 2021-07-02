package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.config.ConfigHolder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface SlaveService {
    void start(ConfigHolder config) throws InterruptedException, IOException, ExecutionException;
}
