package dev.keva.server.protocol.resp.reply;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import static dev.keva.server.protocol.resp.Encoding.numToBytes;

public class IntegerReply implements Reply<Long> {
    public static final char MARKER = ':';
    private static final IntegerReply[] replies = new IntegerReply[512];

    static {
        for (int i = -255; i < 256; i++) {
            replies[i + 255] = new IntegerReply(i);
        }
    }

    private final long integer;

    public IntegerReply(long integer) {
        this.integer = integer;
    }

    public static IntegerReply integer(long integer) {
        if (integer > -256 && integer < 256) {
            return replies[((int) (integer + 255))];
        } else {
            return new IntegerReply(integer);
        }
    }

    @Override
    public Long data() {
        return integer;
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        os.writeByte(MARKER);
        os.writeBytes(numToBytes(integer, true));
    }

    public String toString() {
        return data().toString();
    }
}
