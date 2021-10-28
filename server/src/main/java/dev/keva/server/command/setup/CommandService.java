package dev.keva.server.command.setup;

import dev.keva.server.protocol.resp.Command;
import dev.keva.server.protocol.resp.reply.Reply;

public interface CommandService {
    Reply<?> handleCommand(String name, Command command);
}
