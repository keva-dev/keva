package com.jinyframework.keva.proxy.command;

import io.netty.channel.ChannelHandlerContext;

public class Ping implements CommandHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, String line) {
       ctx.write("PONG");
       ctx.flush();
    }
}
