package dev.keva.server.command.impl.kql.manager;

public class KevaSQLStrUtil {
    public static String escape(String str) {
        return str.replaceAll("^.|.$", "");
    }
}
