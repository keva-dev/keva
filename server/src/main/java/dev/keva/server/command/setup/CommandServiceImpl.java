package dev.keva.server.command.setup;

import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.ErrorReply;
import dev.keva.server.protocol.redis.Reply;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Map;

@Slf4j
public class CommandServiceImpl implements CommandService {

    private final Map<CommandName, CommandHandler> commandHandlerMap;

    public CommandServiceImpl(Map<CommandName, CommandHandler> commandHandlerMap) {
        this.commandHandlerMap = commandHandlerMap;
    }

    @Override
    public Reply<?> handleCommand(String name, Command command) {
        Reply<?> output;
        CommandName commandName;
        try {
            commandName = CommandName.valueOf(name.toUpperCase());
            val handler = commandHandlerMap.get(commandName);
            List<String> objects = command.getObjects();
            output = handler.handle(objects);
        } catch (IllegalArgumentException e) {
            return new ErrorReply("ERR unknown command `" + name.toUpperCase() + "`");
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = new ErrorReply("ERR unknown error: " + e.getMessage());
        }
        return output;
    }
}
