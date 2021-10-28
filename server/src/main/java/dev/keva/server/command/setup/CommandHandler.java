package dev.keva.server.command.setup;

import dev.keva.server.protocol.resp.reply.Reply;

import java.util.List;

@FunctionalInterface
public interface CommandHandler {
    Reply<?> handle(List<String> args);
}
