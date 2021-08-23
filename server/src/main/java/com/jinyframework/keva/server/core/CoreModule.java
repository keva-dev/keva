package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.replication.slave.SlaveService;
import com.jinyframework.keva.server.storage.StorageService;
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
