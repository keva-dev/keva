package dev.keva.server.command.setup;

import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.ErrorReply;
import dev.keva.server.protocol.redis.Reply;
import dev.keva.server.replication.master.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Map;

@Slf4j
public class CommandServiceImpl implements CommandService {

    private final Map<CommandName, CommandHandler> commandHandlerMap;
    private final ReplicationService replicationService;

    public CommandServiceImpl(Map<CommandName, CommandHandler> commandHandlerMap, ReplicationService replicationService) {
        this.commandHandlerMap = commandHandlerMap;
        this.replicationService = replicationService;
    }

    @Override
    public Reply<?> handleCommand(String name, Command command) {
        Reply<?> output;
        try {
            CommandName commandName;
            try {
                commandName = CommandName.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new ErrorReply("ERR unknown command `" + name.toUpperCase() + "`");
            }
            val handler = commandHandlerMap.get(commandName);
            List<String> objects = command.getObjects();
            output = handler.handle(objects);

            // synchronized (this) { // TODO: need to remove sync for single mode
                //output = handler.handle(objects);
                // forward committed change to replicas
                //replicationService.filterAndBuffer(commandName, String.join(" ", objects));
            //}
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = new ErrorReply("ERR unknown error: " + e.getMessage());
        }
        return output;
    }
}
