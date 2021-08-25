package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

@Slf4j
public class FSync implements CommandHandler {
    private final StorageService storageService;
    private final ReplicationService replicationService;

    public FSync(StorageService storageService, ReplicationService replicationService) {
        this.storageService = storageService;
        this.replicationService = replicationService;
    }

    @Override
    public Object handle(List<String> args) {
        // send snapshot to replica
        try {
            // register replica and start buffering commands to forward
            log.info(String.valueOf(args));
            replicationService.addReplica(args.get(0) + ':' + args.get(1));
            return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(storageService.getSnapshotPath() + "/" + "dump.kdb")));
        } catch (IOException e) {
            log.error("FSYNC failed: ", e);
            return "null";
        }
    }
}
