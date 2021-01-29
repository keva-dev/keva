package com.jinyframework.keva.server.command;

import lombok.val;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CommandRegistrar {
    private CommandRegistrar() {
    }

    public static Map<CommandName, CommandHandler> getHandlerMap() {
        return RegistrarHolder.registrar;
    }

    private static final class RegistrarHolder {
        static final Map<CommandName, CommandHandler> registrar = registerCommands();

        private static Map<CommandName, CommandHandler> registerCommands() {
            val map = new HashMap<CommandName, CommandHandler>();
            map.put(CommandName.GET, new Get());
            map.put(CommandName.SET, new Set());
            map.put(CommandName.PING, new Ping());
            map.put(CommandName.INFO, new Info());
            map.put(CommandName.DEL, new Del());

            map.put(CommandName.UNSUPPORTED, new Unsupported());
            return Collections.unmodifiableMap(map);
        }
    }
}
