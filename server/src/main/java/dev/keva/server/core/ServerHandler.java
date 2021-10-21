package dev.keva.server.core;

import dev.keva.server.command.setup.CommandService;
import dev.keva.server.protocol.redis.Command;
import dev.keva.server.protocol.redis.Reply;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<Command> {
    private static final AttributeKey<String> sockIdKey = AttributeKey.newInstance("socketId");

    private final ConcurrentMap<String, ClientInfo> clients;
    private final CommandService commandService;

    public ServerHandler(ConnectionService connectionService, CommandService commandService) {
        this.commandService = commandService;
        clients = connectionService.getClients();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        byte[] bytes = msg.getName();
        String name = new String(bytes, StandardCharsets.UTF_8);
        Reply reply = commandService.handleCommand(name, msg);
        ctx.write(reply);
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
