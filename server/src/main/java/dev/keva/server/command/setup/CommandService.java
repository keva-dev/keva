package dev.keva.server.command.setup;

import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.Reply;

import java.util.ArrayList;
import java.util.Arrays;

public interface CommandService {
    static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    Reply<?> handleCommand(String name, Command command);
}
