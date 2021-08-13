package com.jinyframework.keva.proxy.command;

import java.util.List;

import com.jinyframework.keva.server.command.CommandHandler;

public class Unsupported implements CommandHandler {
    @Override
    public String handle(List<String> args) {
        return "Unsupported command";
    }
}
