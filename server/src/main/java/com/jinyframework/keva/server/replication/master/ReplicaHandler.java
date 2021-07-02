package com.jinyframework.keva.server.replication.master;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;

public class ReplicaHandler extends SimpleChannelInboundHandler<String> {
    private final Promise<Object> resPromise;

    public ReplicaHandler(Promise<Object> resPromise) {
        this.resPromise = resPromise;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        resPromise.setSuccess(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
