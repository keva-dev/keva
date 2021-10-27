package dev.keva.server.command.setup;

import dev.keva.server.command.*;
import dev.keva.store.StorageService;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class CommandRegistrar {
    private final Map<CommandName, CommandHandler> registrar;
    private final StorageService storageService;

    public CommandRegistrar(StorageService storageService) {
        this.storageService = storageService;
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
                entry(CommandName.INFO, new Info()),
                entry(CommandName.DEL, new Del(storageService)),
                entry(CommandName.EXPIRE, new Expire(storageService))
        );
    }
}
