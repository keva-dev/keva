package dev.keva.protocol.resp;

import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public class Command {
    private static final byte LOWER_DIFF = 'a' - 'A';

    private final Object[] objects;

    public Command(Object[] objects, boolean inline) {
        if (inline) {
            byte[] objs = getBytes(objects[0]);
            String[] strings = new String(objs, StandardCharsets.UTF_8).trim().split("\\s+");
            objects = new Object[strings.length];
            for (int i = 0; i < strings.length; i++) {
                objects[i] = getBytes(strings[i]);
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
        byte[] name = getBytes(objects[0]);
        // LowerCase bytes
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        return name;
    }

    private byte[] getBytes(Object object) {
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
