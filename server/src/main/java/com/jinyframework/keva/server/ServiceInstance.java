package com.jinyframework.keva.server;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.command.CommandServiceImpl;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.core.ConnectionServiceImpl;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.replication.master.ReplicationServiceImpl;
import com.jinyframework.keva.server.storage.NoHeapStorageServiceImpl;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.Setter;

@Setter
public final class ServiceInstance {
    private ServiceInstance() {
    }

    public static ReplicationService getReplicationService() {
        return ReplicationServiceHolder.INSTANCE;
    }

    public static ConnectionService getConnectionService() {
        return ConnectionServiceHolder.INSTANCE;
    }

    public static CommandService getCommandService() {
        return CommandServiceHolder.INSTANCE;
    }

    public static StorageService getStorageService() {
        return StorageServiceHolder.INSTANCE;
    }

    private static final class ConnectionServiceHolder {
        private static final ConnectionService INSTANCE = new ConnectionServiceImpl();
    }

    private static final class CommandServiceHolder {
        private static final CommandService INSTANCE = new CommandServiceImpl();
    }

    private static final class StorageServiceHolder {
        private static final StorageService INSTANCE = new NoHeapStorageServiceImpl();
    }

    private static final class ReplicationServiceHolder {
        private static final ReplicationService INSTANCE = new ReplicationServiceImpl();
    }
}
