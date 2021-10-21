package dev.keva.server.core;

import dev.keva.server.command.setup.CommandService;
import dev.keva.server.replication.master.ReplicationService;
import dev.keva.server.replication.slave.SlaveService;
import dev.keva.server.storage.StorageService;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CoreModule {
    private final StorageService storageService;
    private final ConnectionService connectionService;
    private final CommandService commandService;
    private final ReplicationService replicationService;
    private final SlaveService slaveService;
}
