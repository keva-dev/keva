package dev.keva.protocol.resp;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Command implements Serializable {
    private static final byte LOWER_DIFF = 'a' - 'A';

    private final Object[] objects;

    public Command(Object[] objects, boolean inline) {
        if (inline) {
            byte[] objs = ByteUtil.getBytes(objects[0]);
            String[] strings = new String(objs, StandardCharsets.UTF_8).trim().split("\\s+");
            objects = new Object[strings.length];
            for (int i = 0; i < strings.length; i++) {
                objects[i] = ByteUtil.getBytes(strings[i]);
            }
        }
        this.objects = objects;
    }

    public int getLength() {
        int length = 0;
        if (objects != null) {
            length += objects.length;
        }
        return length;
    }

    public byte[] getName() {
        byte[] name = ByteUtil.getBytes(objects[0]);
        // LowerCase bytes
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        return name;
    }

    public void toArguments(Object[] arguments, Class<?>[] types, ChannelHandlerContext ctx) {
        int position = 0;
        for (Class<?> type : types) {
            if (type == ChannelHandlerContext.class) {
                arguments[position] = ctx;
            } else if (type == byte[].class) {
                if (position >= arguments.length) {
                    throw new IllegalArgumentException("wrong number of arguments for '" + new String(getName()).toLowerCase() + "' command");
                }
                if (objects.length - 1 > position) {
                    arguments[position] = objects[1 + position];
                }
            } else {
                // Process Vararg
                boolean isFirstVararg = position == 0;
                int left = isFirstVararg ? (objects.length - position - 1) : (objects.length - 1);
                byte[][] lastArgument = new byte[left][];
                for (int i = 0; i < left; i++) {
                    lastArgument[i] = isFirstVararg ? (byte[]) (objects[i + position + 1]) : (byte[]) (objects[i + position]);
                }
                arguments[position] = lastArgument;
            }
            position++;
        }
    }
}
