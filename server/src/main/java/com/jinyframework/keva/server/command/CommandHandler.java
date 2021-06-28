package com.jinyframework.keva.server.command;

import java.util.List;

@FunctionalInterface
public interface CommandHandler {
    Object handle(CommandContext ctx, List<String> args);
}
