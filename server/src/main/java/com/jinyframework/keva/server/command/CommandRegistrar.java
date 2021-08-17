package com.jinyframework.keva.server.command;

import java.util.Map;

import static java.util.Map.*;

public final class CommandRegistrar {
    private CommandRegistrar() {
    }

    public static Map<CommandName, CommandHandler> getHandlerMap() {
        return RegistrarHolder.registrar;
    }

    private static final class RegistrarHolder {
        static final Map<CommandName, CommandHandler> registrar = registerCommands();

        private static Map<CommandName, CommandHandler> registerCommands() {
            return ofEntries(
                    entry(CommandName.GET, new Get()),
                    entry(CommandName.SET, new Set()),
                    entry(CommandName.PING, new Ping()),
                    entry(CommandName.INFO, new Info()),
                    entry(CommandName.DEL, new Del()),
                    entry(CommandName.EXPIRE, new Expire()),
                    entry(CommandName.FSYNC, new FSync()),

                    entry(CommandName.UNSUPPORTED, new Unsupported())
            );
        }
    }
}
