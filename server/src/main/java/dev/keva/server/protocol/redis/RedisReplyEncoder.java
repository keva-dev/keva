package dev.keva.server.protocol.redis;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RedisReplyEncoder extends MessageToByteEncoder<Reply<?>> {
    @Override
    public void encode(ChannelHandlerContext ctx, Reply msg, ByteBuf out) throws Exception {
        msg.write(out);
    }
}
