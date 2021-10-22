package dev.keva.store;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.val;

@Slf4j
public class NoHeapChronicleMapImpl implements StorageService {
    private ChronicleMap<String, String> chronicleMap;
    private String snapshotDir;

    public NoHeapChronicleMapImpl(NoHeapConfig config) {
        try {
            ChronicleMapBuilder<String, String> mapBuilder = ChronicleMapBuilder.of(String.class, String.class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleSampleSampleKey")
                    .averageValue("SampleSampleSampleSampleSampleSampleValue")
                    .entries(1_000);

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
    public void shutdownGracefully() {
        chronicleMap.close();
    }

    @Override
    public Path getSnapshotPath() {
        return Paths.get(snapshotDir);
    }

    @Override
    public void putString(String key, String val) {
        chronicleMap.put(key, val);
    }

    @Override
    public String getString(String key) {
        return chronicleMap.get(key);
    }

    @Override
    public boolean remove(String key) {
        return chronicleMap.remove(key) != null;
    }
}
