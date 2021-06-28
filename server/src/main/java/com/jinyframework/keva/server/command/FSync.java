package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.replication.ReplicationService;
import com.jinyframework.keva.server.storage.StorageService;
import io.netty.handler.stream.ChunkedFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

@Slf4j
public class FSync implements CommandHandler {
    private final StorageService storageService = ServiceInstance.getStorageService();
    private final ReplicationService replicationService = ServiceInstance.getReplicationService();

    @Override
    public Object handle(CommandContext ctx, List<String> args) {
        // send snapshot to replica
        final File snapshotFile = storageService.getSnapshotPath().toFile();
        try (RandomAccessFile file = new RandomAccessFile(snapshotFile, "r")) {
            // register replica
            replicationService.addReplica(ctx.getRemoteAddr());
            return new ChunkedFile(file);
        } catch (IOException e) {
            log.error("FSYNC failed:", e);
            return null;
        }
    }
}
