package dev.keva.protocol.resp.reply;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ErrorReply implements Reply<String> {
    public static final char MARKER = '-';
    // Pre-defined errors
    public static final ErrorReply SYNTAX_ERROR = new ErrorReply("ERR syntax error");
    public static final ErrorReply ZADD_NX_XX_ERROR = new ErrorReply("ERR XX and NX options at the same time are not compatible");
    public static final ErrorReply ZADD_GT_LT_NX_ERROR = new ErrorReply("GT, LT, and/or NX options at the same time are not compatible");
    public static final ErrorReply ZADD_INCR_ERROR = new ErrorReply("INCR option supports a single increment-element pair");
    public static final ErrorReply ZADD_SCORE_FLOAT_ERROR = new ErrorReply("value is not a valid float");

    private final String error;

    public ErrorReply(String error) {
        this.error = error;
    }

    @Override
    public String data() {
        return error;
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        os.writeByte(MARKER);
        os.writeBytes(error.getBytes(StandardCharsets.UTF_8));
        os.writeBytes(CRLF);
    }

    public String toString() {
        return error;
    }
}
