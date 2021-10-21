package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.annotation.Command;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.storage.StorageService;
import org.reflections.Reflections;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.*;

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

    private void load() {
        Reflections reflections = new Reflections("com.jinyframework.keva.server.command");
        java.util.Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(Command.class);
        Map<String, CommandHandler> challengeClassesMap = commandClasses.stream().collect(
                Collectors.toMap(
                        challengeClass -> challengeClass.getAnnotation(Command.class).value(),
                        challengeClass -> {
                            try {
                                return (CommandHandler) challengeClass.newInstance();
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                );
    }

    private static <T> T createNewInstanceOfClass(Class<T> someClass) {
        try {
            return someClass.newInstance();
        } catch (Exception e) {
            return null; // Ignore
        }
    }
}
