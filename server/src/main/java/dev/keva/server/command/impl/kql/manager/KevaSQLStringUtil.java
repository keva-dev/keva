package dev.keva.server.command.impl.kql.manager;

public class KevaSQLStringUtil {
    public static String escape(String str) {
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.replaceAll("^.|.$", "");
        }
        return str;
    }
}
