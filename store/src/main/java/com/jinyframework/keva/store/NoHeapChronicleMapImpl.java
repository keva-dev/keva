package com.jinyframework.keva.store;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;
import java.io.IOException;

import lombok.val;

@Slf4j
public class NoHeapChronicleMapImpl implements NoHeapStore {
    private ChronicleMap<String, String> chronicleMap;
    private String snapshotDir;

    public NoHeapChronicleMapImpl(NoHeapConfig config) {
        try {
            ChronicleMapBuilder<String, String> mapBuilder = ChronicleMapBuilder.of(String.class, String.class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleKey")
                    .averageValue("SampleValue")
                    .entries(50_000);

            val shouldPersist = config.getSnapshotEnabled();
            if (shouldPersist) {
                val location = config.getSnapshotLocation().equals("./") ? "" : config.getSnapshotLocation() + "/";
                val file = new File(location + "dump.kdb");
                snapshotDir = config.getSnapshotLocation();
                this.chronicleMap = mapBuilder.createPersistedTo(file);
            } else {
                this.chronicleMap = mapBuilder.create();
            }
        } catch (IOException e) {
            log.error("Failed to create ChronicleMap: ", e);
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getFolder() {
        return snapshotDir;
    }

    @Override
    public boolean putInteger(String key, Integer val) {
        return false;
    }

    @Override
    public Integer getInteger(String key) {
        return null;
    }

    @Override
    public boolean putShort(String key, Short val) {
        return false;
    }

    @Override
    public Short getShort(String key) {
        return null;
    }

    @Override
    public boolean putLong(String key, Long val) {
        return false;
    }

    @Override
    public Long getLong(String key) {
        return null;
    }

    @Override
    public boolean putFloat(String key, Float val) {
        return false;
    }

    @Override
    public Float getFloat(String key) {
        return null;
    }

    @Override
    public boolean putDouble(String key, Double val) {
        return false;
    }

    @Override
    public Double getDouble(String key) {
        return null;
    }

    @Override
    public boolean putString(String key, String val) {
        chronicleMap.put(key, val);
        return true;
    }

    @Override
    public String getString(String key) {
        return chronicleMap.get(key);
    }

    @Override
    public boolean putObject(String key, Object msg) {
        return false;
    }

    @Override
    public Object getObject(String key) {
        return null;
    }

    @Override
    public boolean putChar(String key, char val) {
        return false;
    }

    @Override
    public char getChar(String key) {
        return 0;
    }

    @Override
    public boolean remove(String key) {
        chronicleMap.remove(key);
        return true;
    }
}
