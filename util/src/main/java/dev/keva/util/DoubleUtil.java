package dev.keva.util;

public final class DoubleUtil {

    private DoubleUtil(){}

    public static String toString(Double d){
        if (d.equals(Double.POSITIVE_INFINITY)) {
            return "inf";
        }
        if (d.equals(Double.NEGATIVE_INFINITY)) {
            return "-inf";
        }
        return d.toString();
    }
}
