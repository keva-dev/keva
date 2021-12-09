package dev.keva.server.command.impl.kql.manager;

import java.util.Collections;
import java.util.List;

public class KevaSQLResponseUtil {
    public static List<List<Object>> singleSelectResponse(Object object) {
        return Collections.singletonList(Collections.singletonList(object));
    }
}
