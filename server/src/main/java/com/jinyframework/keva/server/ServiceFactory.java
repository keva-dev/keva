package com.jinyframework.keva.server;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.command.CommandServiceImpl;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.core.ConnectionServiceImpl;
import lombok.Setter;

@Setter
public final class ServiceFactory {
    public static ConnectionService connectionService;
    private static CommandService commandService;

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
}
