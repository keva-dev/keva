package com.jinyframework.keva.server;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.command.CommandServiceImpl;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.core.ConnectionServiceImpl;
import com.jinyframework.keva.server.core.SnapShotServiceImpl;
import com.jinyframework.keva.server.core.SnapshotService;
import lombok.Setter;

@Setter
public final class ServiceFactory {
    public static ConnectionService connectionService;
    private static CommandService commandService;
    private static SnapshotService snapshotService;

    public synchronized static ConnectionService getConnectionService() {
        if (connectionService == null) {
            connectionService = new ConnectionServiceImpl();
        }

        return connectionService;
    }

    public synchronized static CommandService getCommandService() {
        if (commandService == null) {
            commandService = new CommandServiceImpl();
        }

        return commandService;
    }

    public synchronized static SnapshotService getSnapshotService() {
        if (snapshotService == null) {
            snapshotService = new SnapShotServiceImpl();
        }

        return snapshotService;
    }
}
