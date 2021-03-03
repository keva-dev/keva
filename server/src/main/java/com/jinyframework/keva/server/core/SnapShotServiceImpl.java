package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.storage.KevaStore;
import com.jinyframework.keva.server.storage.StorageFactory;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.*;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SnapShotServiceImpl implements SnapshotService {
    public static final String snapFileName = "dump.keva";
    private final KevaStore kevaStore = StorageFactory.getKevaStore();

    private void startService(Duration interval, String snapFilePath) {
        final Runnable runnable = () -> {
            log.info("Saving snapshot");
            val entrySetCopy = kevaStore.entrySetCopy();
            try {
                @Cleanup
                val fileOut = new FileOutputStream(snapFilePath);
                @Cleanup
                val objOutStream = new ObjectOutputStream(fileOut);

                val snapMap = entrySetCopy.stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                objOutStream.writeObject(snapMap);
            } catch (IOException e) {
                log.error("Failed to save snapshot:", e);
            }
            log.info("Saved snapshot");
        };
        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(runnable,
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Snapshot service started");
    }

    @Override
    public void start(Duration interval, String snapFilePath) {
        if (snapFilePath == null || snapFilePath.isEmpty()) {
            snapFilePath = Paths.get(".", snapFileName).toString();
        }
        startService(interval, snapFilePath);
    }

    @Override
    public void start(Duration interval) {
        start(interval, null);
    }

    @Override
    public void recover(String snapFilePath) {
        if (snapFilePath == null || snapFilePath.isEmpty()) {
            snapFilePath = Paths.get(".", snapFileName).toString();
        }
        log.info("Recovering data from: {}",snapFilePath);
        try {
            @Cleanup
            val fileIn = new FileInputStream(snapFilePath);
            @Cleanup
            val objInStream = new ObjectInputStream(fileIn);

            @SuppressWarnings("unchecked")
            val snapMap = (HashMap<String, Object>) objInStream.readObject();

            kevaStore.putAll(snapMap);
        } catch (Exception e) {
            log.error("Failed to recover from snapshot:", e);
        }
        log.info("Recovery finished");
    }
}
