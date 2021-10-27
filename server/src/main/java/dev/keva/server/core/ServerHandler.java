package dev.keva.server.core;

import com.google.inject.Inject;
import dev.keva.server.command.setup.CommandService;
import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.Reply;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<Command> {
    private final CommandService commandService;

    public ServerHandler(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) {
        byte[] bytes = msg.getName();
        String name = new String(bytes, StandardCharsets.UTF_8);
        Reply<?> reply = commandService.handleCommand(name, msg);
        ctx.write(reply);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Handler exception caught: ", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Channel channel = ctx.channel();
            log.info("IdleStateEvent triggered, close channel " + channel);
            channelUnregistered(ctx);
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
