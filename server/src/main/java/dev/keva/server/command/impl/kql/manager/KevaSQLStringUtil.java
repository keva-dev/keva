package dev.keva.server.command.impl.kql.manager;

import java.util.Collections;
import java.util.List;

public class KevaSQLStringUtil {
    public static String escape(String str) {
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.replaceAll("^.|.$", "");
        }
        return str;
    }

    public static List<List<Object>> singleSelectResponse(Object object) {
        return Collections.singletonList(Collections.singletonList(object));
    }
}
