package dev.keva.server.protocol.redis;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ErrorReply implements Reply<String> {
    public static final char MARKER = '-';
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
