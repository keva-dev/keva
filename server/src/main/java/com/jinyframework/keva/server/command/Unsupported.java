package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.protocol.redis.ErrorReply;

import java.util.List;

public class Unsupported implements CommandHandler {
    @Override
    public ErrorReply handle(List<String> args) {
        return ErrorReply.NYI_REPLY;
    }
}
