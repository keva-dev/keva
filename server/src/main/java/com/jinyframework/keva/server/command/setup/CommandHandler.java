package com.jinyframework.keva.server.command.setup;

import com.jinyframework.keva.server.protocol.redis.Reply;

import java.util.List;

@FunctionalInterface
public interface CommandHandler {
    Reply<?> handle(List<String> args);
}
