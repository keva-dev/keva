package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.protocol.redis.Command;
import com.jinyframework.keva.server.protocol.redis.ErrorReply;
import com.jinyframework.keva.server.protocol.redis.Reply;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

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
        Reply output;
        try {
            CommandName commandName;
            try {
                commandName = CommandName.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                commandName = CommandName.UNSUPPORTED;
            }
            val handler = commandHandlerMap.get(commandName);
            synchronized (this) {
                output = handler.handle(command.getObjects());
                // forward committed change to replicas
                // replicationService.filterAndBuffer(command, line);
            }
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = new ErrorReply("Error while handling command: " + e.getMessage());
        }
        return output;
    }
}
