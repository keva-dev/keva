package dev.keva.protocol.resp;

import java.nio.charset.StandardCharsets;

public class ByteUtil {
    public static byte[] getBytes(Object object) {
        byte[] argument;
        if (object == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        } else if (object instanceof byte[]) {
            argument = (byte[]) object;
        } else if (object instanceof String) {
            argument = ((String) object).getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + object.getClass());
        }
        return argument;
    }
}
