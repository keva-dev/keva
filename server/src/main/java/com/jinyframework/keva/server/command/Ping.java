package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.command.setup.CommandHandler;
import com.jinyframework.keva.server.protocol.redis.StatusReply;

import java.util.List;

public class Ping implements CommandHandler {
    @Override
    public StatusReply handle(List<String> args) {
        return new StatusReply("PONG");
    }
}
