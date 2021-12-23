package dev.keva.util;

public final class Constants {

    public static final int FLAG_XX = 1;
    public static final int FLAG_NX = 1 << 1;
    public static final int FLAG_GT = 1 << 2;
    public static final int FLAG_LT = 1 << 3;
    public static final int FLAG_INCR = 1 << 4;
    public static final int FLAG_CH = 1 << 5;

    public static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors() * 2;

    private Constants() {
    }
}
