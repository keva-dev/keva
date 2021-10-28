package dev.keva.server.protocol.resp.reply;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StatusReply implements Reply<String> {
    public static final char MARKER = '+';
    public static final StatusReply OK = new StatusReply("OK");
    private final String status;
    private final byte[] statusBytes;

    public StatusReply(String status) {
        this.status = status;
        this.statusBytes = status.getBytes(StandardCharsets.UTF_8);
    }

    public StatusReply(byte[] status) {
        this.status = null;
        this.statusBytes = status;
    }

    @Override
    public String data() {
        return status;
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        os.writeByte(MARKER);
        os.writeBytes(statusBytes);
        os.writeBytes(CRLF);
    }

    public String toString() {
        return status;
    }
}
