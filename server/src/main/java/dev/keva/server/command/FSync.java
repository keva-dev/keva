package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.RawReply;
import dev.keva.server.replication.master.ReplicationService;
import dev.keva.server.storage.StorageService;
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
    public RawReply handle(List<String> args) {
        // send snapshot to replica
        try {
            // register replica and start buffering commands to forward
            log.info(String.valueOf(args));
            replicationService.addReplica(args.get(1) + ':' + args.get(2));
            return new RawReply(Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(storageService.getSnapshotPath() + "/" + "dump.kdb"))));
        } catch (IOException e) {
            log.error("FSYNC failed: ", e);
            return new RawReply("null");
        }
    }
}
