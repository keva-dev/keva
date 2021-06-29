package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.storage.StorageService;
import io.netty.channel.DefaultFileRegion;
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
    public Object handle(List<String> args) {
        // send snapshot to replica
        final File snapshotFile = storageService.getSnapshotPath().toFile();
        final RandomAccessFile file;
        try {
            file = new RandomAccessFile(snapshotFile, "r");
            // register replica and start buffering commands to forward
            replicationService.addReplica(args.get(0));
            return new DefaultFileRegion(file.getChannel(), 0, file.length());
        } catch (IOException e) {
            log.error("FSYNC failed:", e);
            return null;
        }
    }
}
