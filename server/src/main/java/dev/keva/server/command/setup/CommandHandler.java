package dev.keva.server.command.setup;

import dev.keva.server.protocol.redis.Reply;

import java.util.List;

@FunctionalInterface
public interface CommandHandler {
    Reply<?> handle(List<String> args);
}
