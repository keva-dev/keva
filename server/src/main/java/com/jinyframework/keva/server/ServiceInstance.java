package com.jinyframework.keva.server;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.command.CommandServiceImpl;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.core.ConnectionServiceImpl;
import com.jinyframework.keva.server.storage.StorageServiceImpl;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.Setter;

@Setter
public final class ServiceInstance {
    private ServiceInstance() {
    }

    private static final class ConnectionServiceHolder {
        private static final ConnectionService INSTANCE = new ConnectionServiceImpl();
    }

    private static final class CommandServiceHolder {
        private static final CommandService INSTANCE = new CommandServiceImpl();
    }

    private static final class StorageServiceHolder {
        private static final StorageService INSTANCE = new StorageServiceImpl();
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
}
