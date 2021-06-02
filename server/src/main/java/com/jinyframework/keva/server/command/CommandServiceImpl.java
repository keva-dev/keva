package com.jinyframework.keva.server.command;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;

import static com.jinyframework.keva.server.command.CommandRegistrar.getHandlerMap;

@Slf4j
public class CommandServiceImpl implements CommandService {
    private final Map<CommandName, CommandHandler> commandHandlerMap = getHandlerMap();

    @Override
    public Object handleCommand(String line) {
        Object output;
        try {
            val args = CommandService.parseTokens(line);
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
        return output;
    }
}
