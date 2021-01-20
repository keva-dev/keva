package com.jinyframework.keva.server;

import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.core.ConnectionService;
import lombok.Setter;

@Setter
public final class ServiceFactory {

    private ServiceFactory() {
    }

    public static ConnectionService connectionService() {
        return ConnectionServiceHolder.connectionService;
    }

    public static CommandService commandService() {
        return CommandServiceHolder.commandService;
    }

    private static final class ConnectionServiceHolder {
        static final ConnectionService connectionService = new ConnectionService();
    }

    private static final class CommandServiceHolder {
        static final CommandService commandService = new CommandService();
    }
}
