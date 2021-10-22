package dev.keva.store;

import java.nio.file.Path;

public interface StorageService {

    void shutdownGracefully();

    Path getSnapshotPath();

    void putString(String key, String val);

    String getString(String key);

    boolean remove(String key);
}
