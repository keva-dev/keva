package com.jinyframework.keva.engine;

import java.io.File;
import java.io.IOException;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

public class ChronicleStringKevaMap implements KevaMap<String, String>, AutoCloseable {
    private static final long numOfEntries = 100;
    private static final String AVERAGE_KEY = "this-is-test-key";
    private static final String AVERAGE_VALUE = "this-is-test-value";

    private ChronicleMap<String, String> chronicleStringMap = null;

    public static ChronicleMapBuilder<String, String> initBuilder() {
        return ChronicleMapBuilder
                .of(String.class, String.class)
                .name("chronicle-map")
                .entries(numOfEntries)
                .averageKey(AVERAGE_KEY)
                .averageValue(AVERAGE_VALUE);
    }

    public ChronicleStringKevaMap() {
        this.chronicleStringMap = initBuilder().create();
    }

    public ChronicleStringKevaMap(String snapshotLocation) throws IOException {
        // First create the directory
        File filePath = new File(snapshotLocation +
                                 (snapshotLocation.isEmpty() ? "" : File.separator)
                                 + "keva.dump");
        if (filePath.exists()) {
            this.chronicleStringMap = initBuilder()
                    .recoverPersistedTo(filePath, true);
        } else {
            this.chronicleStringMap = initBuilder()
                    .createPersistedTo(filePath);
        }
    }

    @Override
    public String get(Object key) {
        return chronicleStringMap.get(key.toString());
    }

    @Override
    public String put(String key, String value) {
        return chronicleStringMap.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return chronicleStringMap.remove(key.toString());
    }

    @Override
    public void close() {
        chronicleStringMap.close();
    }
}
