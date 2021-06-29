package com.jinyframework.keva.server.replication.slave;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class SyncHandler extends SimpleChannelInboundHandler<Object> {
    private final Path snapshotPath;

    public SyncHandler(String snapshotPath) {
        this.snapshotPath = Path.of(snapshotPath);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        final byte[] bytes = (byte[]) msg;
        Files.write(snapshotPath, bytes);
        log.info("Finished writing snapshot file to " + snapshotPath);
        ctx.close();
    }
}
