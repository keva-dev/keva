package dev.keva.server.command.setup;

import dev.keva.server.command.*;
import dev.keva.server.core.ConnectionService;
import dev.keva.server.replication.master.ReplicationService;
import dev.keva.server.storage.StorageService;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class CommandRegistrar {
    private final Map<CommandName, CommandHandler> registrar;
    private final StorageService storageService;
    private final ReplicationService replicationService;
    private final ConnectionService connectionService;

    public CommandRegistrar(StorageService storageService,
                            ReplicationService replicationService,
                            ConnectionService connectionService) {
        this.storageService = storageService;
        this.replicationService = replicationService;
        this.connectionService = connectionService;
        registrar = registerCommands();
    }

    public Map<CommandName, CommandHandler> getHandlerMap() {
        return registrar;
    }

    private Map<CommandName, CommandHandler> registerCommands() {
        return ofEntries(
                entry(CommandName.GET, new Get(storageService)),
                entry(CommandName.SET, new Set(storageService)),
                entry(CommandName.PING, new Ping()),
                entry(CommandName.INFO, new Info(replicationService, connectionService)),
                entry(CommandName.DEL, new Del(storageService)),
                entry(CommandName.EXPIRE, new Expire(storageService)),
                entry(CommandName.FSYNC, new FSync(storageService, replicationService)),
                entry(CommandName.PSYNC, new PSync(replicationService))
        );
    }
}
