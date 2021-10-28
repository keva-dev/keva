package dev.keva.store;

import java.nio.file.Path;

public interface KevaDatabase {

    void shutdownGracefully();

    Path getSnapshotPath();

    void put(byte[] key, byte[] val);

    byte[] get(byte[] key);

    boolean remove(byte[] key);
}
