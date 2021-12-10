package dev.keva.core.command.mapping;

import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.reply.Reply;
import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface CommandWrapper {
    Reply<?> execute(ChannelHandlerContext ctx, Command command) throws InterruptedException;
}
