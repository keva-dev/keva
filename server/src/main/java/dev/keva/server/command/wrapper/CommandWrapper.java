package dev.keva.server.command.wrapper;

import dev.keva.server.protocol.resp.Command;
import dev.keva.server.protocol.resp.reply.Reply;
import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface CommandWrapper {
    Reply<?> execute(ChannelHandlerContext ctx, Command command);
}
