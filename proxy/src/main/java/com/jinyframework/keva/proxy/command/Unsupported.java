package com.jinyframework.keva.proxy.command;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;

public class Unsupported implements CommandHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, String line) {
        ctx.write("Unsupported command");
        ctx.flush();
    }
}
