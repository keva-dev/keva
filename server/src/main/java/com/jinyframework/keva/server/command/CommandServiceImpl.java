package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.master.ReplicationService;
import io.netty.channel.ChannelHandlerContext;
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
    public Object handleCommand(ChannelHandlerContext ctx, String line) {
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

            // forward committed change to replicas
            replicationService.filterAndBuffer(command, line);
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = "ERROR";
        }
        return output;
    }
}
