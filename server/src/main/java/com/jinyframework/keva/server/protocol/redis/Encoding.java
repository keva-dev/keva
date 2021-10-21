package com.jinyframework.keva.server.protocol.redis;

public class Encoding {
    public static final char LF = '\n';
    public static final char CR = '\r';
    public static final byte[] NEG_ONE = convert(-1, false);
    public static final byte[] NEG_ONE_WITH_CRLF = convert(-1, true);

    private static final int NUM_MAP_LENGTH = 256;
    private static final byte[][] numMap = new byte[NUM_MAP_LENGTH][];
    private static final byte[][] numMapWithCRLF = new byte[NUM_MAP_LENGTH][];

    static {
        for (int i = 0; i < NUM_MAP_LENGTH; i++) {
            numMap[i] = convert(i, false);
        }
    }

    static {
        for (int i = 0; i < NUM_MAP_LENGTH; i++) {
            numMapWithCRLF[i] = convert(i, true);
        }
    }

    public static byte[] numToBytes(long value) {
        return numToBytes(value, false);
    }

    public static byte[] numToBytes(long value, boolean withCRLF) {
        if (value >= 0 && value < NUM_MAP_LENGTH) {
            int index = (int) value;
            return withCRLF ? numMapWithCRLF[index] : numMap[index];
        } else if (value == -1) {
            return withCRLF ? NEG_ONE_WITH_CRLF : NEG_ONE;
        }
        return convert(value, withCRLF);
    }

    private static byte[] convert(long value, boolean withCRLF) {
        boolean negative = value < 0;
        long abs = Math.abs(value);
        int index = (value == 0 ? 0 : (int) Math.log10(abs)) + (negative ? 2 : 1);
        byte[] bytes = new byte[withCRLF ? index + 2 : index];
        if (withCRLF) {
            bytes[index] = CR;
            bytes[index + 1] = LF;
        }
        if (negative) bytes[0] = '-';
        long next = abs;
        while ((next /= 10) > 0) {
            bytes[--index] = (byte) ('0' + (abs % 10));
            abs = next;
        }
        bytes[--index] = (byte) ('0' + abs);
        return bytes;
    }

    public static long bytesToNum(byte[] bytes) {
        int length = bytes.length;
        if (length == 0) {
            throw new IllegalArgumentException("value is not an integer or out of range");
        }
        int position = 0;
        int sign;
        int read = bytes[position++];
        if (read == '-') {
            read = bytes[position++];
            sign = -1;
        } else {
            sign = 1;
        }
        long number = 0;
        do {
            int value = read - '0';
            if (value >= 0 && value < 10) {
                number *= 10;
                number += value;
            } else {
                throw new IllegalArgumentException("value is not an integer or out of range");
            }
            if (position == length) {
                return number * sign;
            }
            read = bytes[position++];
        } while (true);
    }

}
