package dev.keva.server.replication.slave;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyncHandler extends SimpleChannelInboundHandler<Object> {
    private final Promise<Object> resPromise;

    public SyncHandler(Promise<Object> resPromise) {
        this.resPromise = resPromise;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        resPromise.setSuccess(msg);
        ctx.close();
    }
}
