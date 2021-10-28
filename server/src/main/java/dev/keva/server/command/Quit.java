package dev.keva.server.command;

import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.resp.reply.StatusReply;

import java.util.List;

public class Quit implements CommandHandler {
    @Override
    public StatusReply handle(List<String> args) {
        return new StatusReply("OK");
    }
}
