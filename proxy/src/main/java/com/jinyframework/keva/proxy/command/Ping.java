package com.jinyframework.keva.proxy.command;

import java.util.List;

import command.CommandHandler;

public class Ping implements CommandHandler {
    @Override
    public String handle(List<String> args) {
        return "PONG";
    }
}
