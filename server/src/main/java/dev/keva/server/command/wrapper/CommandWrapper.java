package dev.keva.server.command.wrapper;

import dev.keva.server.protocol.resp.Command;
import dev.keva.server.protocol.resp.reply.Reply;

@FunctionalInterface
public interface CommandWrapper {
    Reply<?> execute(Command command);
}
