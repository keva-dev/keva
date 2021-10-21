package dev.keva.server.replication.slave;

import dev.keva.server.config.ConfigHolder;

public interface SlaveService {
    void start(ConfigHolder config) throws Exception;
}
