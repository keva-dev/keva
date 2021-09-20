package com.jinyframework.keva.proxy.command;

import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class CommandServiceImpl implements CommandService {
    private final Map<CommandName, CommandHandler> commandHandlerMap = CommandRegistrar.getHandlerMap();

    @Override
    public void handleCommand(ChannelHandlerContext ctx, String line) {
        try {
            val args = CommandService.parseTokens(line);
            CommandName command;
            try {
                command = CommandName.valueOf(args.get(0).toUpperCase());
            } catch (IllegalArgumentException e) {
                command = CommandName.UNSUPPORTED;
            }

            val handler = commandHandlerMap.get(command);
            handler.handle(ctx, line);
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
        }
    }
}
