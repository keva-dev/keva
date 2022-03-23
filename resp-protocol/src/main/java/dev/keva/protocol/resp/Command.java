package dev.keva.protocol.resp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Recycler;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

public class Command {
    private static final byte LOWER_DIFF = 'a' - 'A';

    private static final Recycler<Command> RECYCLER = new Recycler<Command>() {
        protected Command newObject(Recycler.Handle<Command> handle) {
            return new Command(handle);
        }
    };

    private final Recycler.Handle<Command> handle;

    @Getter
    private byte[][] objects;

    private Command(Recycler.Handle<Command> handle) {
        this.handle = handle;
    }

    public static Command newInstance(byte[][] objects, boolean inline) {
        if (objects == null) {
            throw new IllegalArgumentException("objects must not be null");
        }
        if (objects.length == 0) {
            throw new IllegalArgumentException("objects must not be empty");
        }

        Command command = RECYCLER.get();
        if (inline) {
            byte[] objs = objects[0];
            String[] strings = new String(objs, StandardCharsets.UTF_8).trim().split("\\s+");
            objects = new byte[strings.length][];
            for (int i = 0; i < strings.length; i++) {
                objects[i] = ByteUtil.getBytes(strings[i]);
            }
        }
        command.objects = objects;
        // LowerCase bytes
        for (int i = 0; i < objects[0].length; i++) {
            byte b = objects[0][i];
            if (b >= 'A' && b <= 'Z') {
                objects[0][i] = (byte) (b + LOWER_DIFF);
            }
        }
        return command;
    }

    public int getLength() {
        int length = 0;
        if (objects != null) {
            length += objects.length;
        }
        return length;
    }

    public long getByteSize() {
        long size = 0;
        if (objects != null) {
            for (byte[] object : objects) {
                size += object.length;
            }
        }
        return size;
    }

    public byte[] getName() {
        return objects[0];
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
                    lastArgument[i] = isFirstVararg ? objects[i + position + 1] : objects[i + position];
                }
                arguments[position] = lastArgument;
            }
            position++;
        }
    }

    public void recycle() {
        objects = null;
        handle.recycle(this);
    }
}
