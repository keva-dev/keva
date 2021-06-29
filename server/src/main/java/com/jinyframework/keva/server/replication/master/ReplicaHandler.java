package com.jinyframework.keva.server.replication.master;

import com.jinyframework.keva.server.ServiceInstance;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ReplicaHandler extends SimpleChannelInboundHandler<String> {
    private final ReplicationService replicationService = ServiceInstance.getReplicationService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        final String key = String.valueOf(ctx.channel().remoteAddress());
        final ReplicaInfo replicaInfo = replicationService.getReplicas().get(key);
        final long now = System.currentTimeMillis();
        replicaInfo.getLastCommunicated().getAndUpdate(old -> Math.max(old, now));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
