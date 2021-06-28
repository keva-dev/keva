package com.jinyframework.keva.server.command;

import java.util.List;

public class Unsupported implements CommandHandler {
    @Override
    public String handle(CommandContext ctx, List<String> args) {
        return "Unsupported command";
    }
}
