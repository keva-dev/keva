package com.jinyframework.keva.server.command;

import java.util.List;

public class Ping implements CommandHandler{
    @Override
    public String handle(List<String> args) {
        return "PONG";
    }
}
