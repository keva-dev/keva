package dev.keva.server.protocol.redis;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RawReply implements Reply<Object> {
    private final Object o;

    public RawReply(Object o) {
        this.o = o;
    }

    @Override
    public Object data() {
        return o;
    }

    @Override
    public void write(ByteBuf os) throws IOException {
        if (o instanceof String) {
            os.writeBytes(((String) o).getBytes(StandardCharsets.US_ASCII));
            os.writeBytes(CRLF);
        } else if (o instanceof byte[]) {
            os.writeBytes((byte[]) o);
            os.writeBytes(CRLF);
        }
    }
}
