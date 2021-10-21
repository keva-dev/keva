package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.protocol.redis.Command;
import com.jinyframework.keva.server.protocol.redis.Reply;

import java.util.ArrayList;
import java.util.Arrays;

public interface CommandService {
    static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    Reply<?> handleCommand(String name, Command command);
}
