package dev.keva.server.command.impl.kql.manager;

import java.util.List;

public class ColumnFinder {
    public static int findColumn(String columnName, List<KevaColumnDefinition> kevaColumns) {
        for (int i = 0; i < kevaColumns.size(); i++) {
            KevaColumnDefinition kevaColumn = kevaColumns.get(i);
            if (kevaColumn.name.equals(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
