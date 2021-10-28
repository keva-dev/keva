package dev.keva.server.command.setup;

import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.Reply;

public interface CommandService {
    Reply<?> handleCommand(String name, Command command);
}
