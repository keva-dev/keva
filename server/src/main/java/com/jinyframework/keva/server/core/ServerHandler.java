package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.command.CommandService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private final ConcurrentMap<String, ClientInfo> clients = ServiceInstance.getConnectionService().getClients();
    private final CommandService commandService = ServiceInstance.getCommandService();

    private final AttributeKey<String> sockIdKey = AttributeKey.newInstance("socketId");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        Object res = commandService.handleCommand(ctx, msg);
        if (res == null) {
            res = "null";
        }
        ctx.write(res);
        ctx.writeAndFlush("\n");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        final String socketId = UUID.randomUUID().toString();
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        ctx.channel().attr(sockIdKey).setIfAbsent(socketId);
        clients.put(socketId, ClientInfo.builder()
                                        .id(socketId)
                                        .build());
        log.debug("{} {} connected", remoteAddr, socketId);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        final String socketId = ctx.channel().attr(sockIdKey).get();
        clients.remove(socketId);
        log.debug("{} {} disconnected", remoteAddr, socketId);
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
}
