package com.jinyframework.keva.server.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;

import static com.jinyframework.keva.server.command.CommandRegistrar.getHandlerMap;

@Slf4j
public class CommandServiceImpl implements CommandService {
    private final Map<CommandName, CommandHandler> commandHandlerMap = getHandlerMap();

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
            final CommandContext cmdCtx = CommandContext.builder()
                                                        .remoteAddr(ctx.channel().remoteAddress())
                                                        .build();

            // Need this handler for file transfer so only add when necessary,
            // because it's not a sharable handler that can be instantiated once
            if (CommandName.FSYNC == command) {
                ctx.channel().pipeline().addLast(new ChunkedWriteHandler());
            }
            output = handler.handle(cmdCtx, args);
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = "ERROR";
        }
        return output;
    }
}
