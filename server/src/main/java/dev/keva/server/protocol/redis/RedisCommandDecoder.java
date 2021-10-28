package dev.keva.server.protocol.redis;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static dev.keva.server.protocol.redis.RedisReplyDecoder.readLong;

public class RedisCommandDecoder extends ReplayingDecoder<Void> {

    private byte[][] bytes;
    private int arguments = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bytes != null) {
            int numArgs = bytes.length;
            for (int i = arguments; i < numArgs; i++) {
                if (in.readByte() == '$') {
                    long l = readLong(in);
                    if (l > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
                    }
                    int size = (int) l;
                    bytes[i] = new byte[size];
                    in.readBytes(bytes[i]);
                    if (in.bytesBefore((byte) '\r') != 0) {
                        throw new RuntimeException("Argument doesn't end in CRLF");
                    }
                    in.skipBytes(2);
                    arguments++;
                    checkpoint();
                } else {
                    throw new IOException("Unexpected character");
                }
            }
            try {
                out.add(new Command(bytes));
            } finally {
                bytes = null;
                arguments = 0;
            }
        } else if (in.readByte() == '*') {
            long l = readLong(in);
            if (l > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
            }
            int numArgs = (int) l;
            if (numArgs < 0) {
                throw new RuntimeException("Invalid size: " + numArgs);
            }
            bytes = new byte[numArgs][];
            checkpoint();
            decode(ctx, in, out);
        } else {
            in.readerIndex(in.readerIndex() - 1);;
            byte[][] b = new byte[1][];
            ByteBuf buf = in.readBytes(in.bytesBefore((byte) '\n'));
            b[0] = new byte[buf.readableBytes()];
            buf.getBytes(0, b[0]);
            // Support both CRLF and LF
            if (b[0][b[0].length - 1] == 13) {
                b[0] = Arrays.copyOf(b[0], b[0].length - 1);
            }
            in.skipBytes(1);
            out.add(new Command(b, true));
        }
    }
}
