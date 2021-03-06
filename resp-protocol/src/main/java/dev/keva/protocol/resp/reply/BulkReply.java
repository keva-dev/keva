package dev.keva.protocol.resp.reply;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static dev.keva.protocol.resp.Encoding.numToBytes;

public class BulkReply implements Reply<ByteBuf> {
    public static final BulkReply NIL_REPLY = new BulkReply();
    public static final BulkReply POSITIVE_INFINITY_REPLY = new BulkReply("inf");
    public static final BulkReply NEGATIVE_INFINITY_REPLY = new BulkReply("-inf");

    public static final char MARKER = '$';
    private final ByteBuf bytes;
    private final int capacity;

    private BulkReply() {
        bytes = null;
        capacity = -1;
    }

    public BulkReply(byte[] bytes) {
        this.bytes = Unpooled.wrappedBuffer(bytes);
        capacity = bytes.length;
    }

    public BulkReply(String str) {
        this(str.getBytes(StandardCharsets.UTF_8));
    }

    public BulkReply(ByteBuf bytes) {
        this.bytes = bytes;
        capacity = bytes.capacity();
    }

    @Override
    public ByteBuf data() {
        return bytes;
    }

    public String asUTF8String() {
        if (bytes == null) return null;
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public String asString(Charset charset) {
        if (bytes == null) return null;
        return bytes.toString(charset);
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        os.writeByte(MARKER);
        os.writeBytes(numToBytes(capacity, true));
        if (capacity > 0) {
            os.writeBytes(bytes.array());
            os.writeBytes(CRLF);
        }
        if (capacity == 0) {
            os.writeBytes(CRLF);
        }
    }

    public String toString() {
        return asUTF8String();
    }
}
