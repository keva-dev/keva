package dev.keva.protocol.resp.reply;

import dev.keva.protocol.resp.Encoding;
import dev.keva.protocol.resp.RedisReplyDecoder;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultiBulkReply implements Reply<Reply<?>[]> {
    public static final char MARKER = '*';
    public static final MultiBulkReply EMPTY = new MultiBulkReply(new Reply[0]);

    private Reply<?>[] replies;
    private int size = -2;
    private int index = 0;

    public MultiBulkReply() {
    }

    public MultiBulkReply(Reply<?>[] replies) {
        this.replies = replies;
        size = replies.length;
    }

    public void read(RedisReplyDecoder rd, ByteBuf is) throws IOException {
        if (size == -2) {
            long l = RedisReplyDecoder.readLong(is);
            if (l > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
            }
            size = (int) l;
            if (size == -1) {
                replies = null;
            } else {
                if (size < 0) {
                    throw new IllegalArgumentException("Invalid size: " + size);
                }
                replies = new Reply[size];
            }
            rd.checkpoint();
        }
        for (int i = index; i < size; i++) {
            replies[i] = rd.readReply(is);
            index = i + 1;
            rd.checkpoint();
        }
    }

    @Override
    public Reply<?>[] data() {
        return replies;
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        os.writeByte(MARKER);
        if (replies == null) {
            os.writeBytes(Encoding.NEG_ONE_WITH_CRLF);
        } else {
            os.writeBytes(Encoding.numToBytes(replies.length, true));
            for (Reply<?> reply : replies) {
                reply.write(os);
            }
        }
    }

    public List<String> asStringList(Charset charset) {
        if (replies == null) return null;
        List<String> strings = new ArrayList<>(replies.length);
        for (Reply<?> reply : replies) {
            if (reply instanceof BulkReply) {
                strings.add(((BulkReply) reply).asString(charset));
            } else {
                throw new IllegalArgumentException("Could not convert " + reply + " to a string");
            }
        }
        return strings;
    }

    public String toString() {
        return asStringList(StandardCharsets.UTF_8).toString();
    }
}
