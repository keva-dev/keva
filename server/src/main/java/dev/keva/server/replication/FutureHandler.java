package dev.keva.server.replication;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
@ChannelHandler.Sharable
public class FutureHandler extends SimpleChannelInboundHandler<String> {
    private final LinkedBlockingDeque<CompletableFuture<Object>> futureQueue;

    public FutureHandler(LinkedBlockingDeque<CompletableFuture<Object>> futureQueue) {
        this.futureQueue = futureQueue;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        final CompletableFuture<Object> future = futureQueue.poll();
        if (future != null) {
            future.complete(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        final CompletableFuture<Object> future = futureQueue.poll();
        if (future != null) {
            future.completeExceptionally(cause);
        }
        ctx.close();
    }

}
