package com.jinyframework.keva.server.core;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.command.CommandService;
import com.jinyframework.keva.server.storage.SocketMapInstance;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@ChannelHandler.Sharable
public class NioServerHandler extends SimpleChannelInboundHandler<String> {
    private final Map<String, ServerSocket> socketMap = SocketMapInstance.getSocketHashMap();

    private final CommandService commandService = ServiceInstance.getCommandService();

    private final AttributeKey<String> sockIdKey = AttributeKey.newInstance("socketId");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        final Object res = commandService.handleCommand(msg);
        final String resStr = res + "\n";
        ctx.write(resStr);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        String socketId = UUID.randomUUID().toString();
        String remoteAddr = ctx.channel().remoteAddress().toString();
        ctx.channel().attr(sockIdKey).setIfAbsent(socketId);
        socketMap.put(socketId, ServerSocket.builder()
                                            .id(socketId)
                                            .build());
        log.debug("{} {} connected", remoteAddr, socketId);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        String remoteAddr = ctx.channel().remoteAddress().toString();
        String socketId = ctx.channel().attr(sockIdKey).get();
        socketMap.remove(socketId);
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
