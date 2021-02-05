package com.jinyframework.keva.server.core;

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

import static com.jinyframework.keva.server.storage.StorageFactory.hashStore;

@Slf4j
public class SnapShotServiceImpl implements SnapshotService {
    public static final String snapFileName = "dump.keva";
    private final Map<String, String> hashStore = hashStore();

    @Override
    public void start(Duration interval, String fileDir) {
        if (fileDir == null || fileDir.isEmpty()) {
            fileDir = ".";
        }
        val finalFileDir = fileDir;
        final Runnable runnable = () -> {
            log.info("Saving snapshot");
            val entrySetCopy = new HashMap<>(hashStore).entrySet();
            try {
                val snapFilePath = Paths.get(finalFileDir, snapFileName);
                @Cleanup
                val fileOut = new FileOutputStream(snapFilePath.toString());
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
    public void start(Duration interval) {
        start(interval, "./");
    }

    @Override
    public void recover(String snapFilePath) {
        if (snapFilePath == null || snapFilePath.isEmpty()) {
            snapFilePath = Paths.get(".", snapFileName).toString();
        }
        log.info("Recovering hash map from file");
        try {
            @Cleanup
            val fileIn = new FileInputStream(snapFilePath);
            @Cleanup
            val objInStream = new ObjectInputStream(fileIn);

            val snapMap = (HashMap<String, String>) objInStream.readObject();
            hashStore.putAll(snapMap);
        } catch (Exception e) {
            log.error("Failed to recover from snapshot:", e);
        }
        log.info("Recovery finished");
    }
}
