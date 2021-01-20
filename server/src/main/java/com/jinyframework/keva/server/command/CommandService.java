package com.jinyframework.keva.server.command;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Slf4j
public class CommandService {

    private final Map<CommandName, CommandHandler> commandHandlerMap = CommandRegistrar.getRegistrar();


    private static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    public void handleCommand(PrintWriter socketOut, String line) {
        Object output;
        try {
            val args = parseTokens(line);
            CommandName command;
            try {
                command = CommandName.valueOf(args.get(0).toUpperCase());
            } catch (IllegalArgumentException e) {
                command = CommandName.UNSUPPORTED;
            }
            val handler = commandHandlerMap.get(command);
            args.remove(0);
            output = handler.handle(args);
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = "ERROR";
        }
        socketOut.println(output);
        socketOut.flush();
    }
}
