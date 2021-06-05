package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.command.CommandService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class NioServerHandler extends SimpleChannelInboundHandler<String> {
    String socketId = null;
    private final CommandService commandService = ServiceInstance.getCommandService();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        final Object res = commandService.handleCommand(msg);
        final Object resStr = String.valueOf(res) + "\n";
        ctx.write(resStr);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        socketId = UUID.randomUUID().toString();
        String remoteAddr = ctx.channel().remoteAddress().toString();
        log.info("{} {} connected", remoteAddr, socketId);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        String remoteAddr = ctx.channel().remoteAddress().toString();
        log.info("{} {} disconnected", remoteAddr, socketId);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Handler exception caught: ",cause);
        ctx.close();
    }
}
