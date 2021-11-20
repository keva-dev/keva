package dev.keva.server.command.impl.kql.manager;

import lombok.AllArgsConstructor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;

public class KqlManager {
    private final Map<String, List<KevaColumnDefinition>> metadata = new HashMap<>();
    private final Map<String, List<Object>> tableData = new HashMap<>();

    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    public void create(Statement stmt) {
        CreateTable createTable = (CreateTable) stmt;
        String tableName = createTable.getTable().getName();
        if (metadata.get(tableName) != null) {
            throw new KevaSQLException("table " + tableName + " already exists!");
        }
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
        List<KevaColumnDefinition> kevaColumns = new ArrayList<>();
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            KevaColumnDefinition kevaColumn = new KevaColumnDefinition
                    (columnDefinition.getColumnName(), columnDefinition.getColDataType().getDataType());
            kevaColumns.add(kevaColumn);
        }
        metadata.put(tableName, kevaColumns);
    }

    @AllArgsConstructor
    public static class KevaColumnDefinition {
        public String name;
        public String type;
    }

    public void insert(Statement stmt) {
        Insert insertStatement = (Insert) stmt;
        Table table = insertStatement.getTable();
        String tableName = table.getName();
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist!");
        }
        List<Expression> insertValuesExpression = ((ExpressionList) insertStatement.getItemsList()).getExpressions();
        List<String> values = new ArrayList<>();
        for (Expression expression : insertValuesExpression) {
            values.add(expression.toString());
        }

        List<Object> result = new ArrayList<>();
        List<Column> insertColumns = insertStatement.getColumns();
        if (insertColumns != null) {
            for (Column column : insertColumns) {
                int index = findColumn(column.getColumnName(), columnDefinitions);
                if (index == -1) {
                    throw new KevaSQLException("column " + column + " does not exist!");
                }
                String type = columnDefinitions.get(index).type;
                String value = values.get(index);
                result.add(addResult(type, value));
            }
        } else {
            for (int i = 0; i < columnDefinitions.size(); i++) {
                String type = columnDefinitions.get(i).type;
                String value = values.get(i);
                result.add(addResult(type, value));
            }
        }

        String uuid = UUID.randomUUID().toString();
        tableData.computeIfAbsent(tableName, k -> new ArrayList<>());
        tableData.put(tableName + ":" + uuid, result);
    }

    private int findColumn(String columnName, List<KevaColumnDefinition> kevaColumns) {
        for (int i = 0; i < kevaColumns.size(); i++) {
            KevaColumnDefinition kevaColumn = kevaColumns.get(i);
            if (kevaColumn.name.equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private Object addResult(String type, String value) {
        if (type.equals("STRING") || type.equals("TEXT")) {
            return value.replaceAll("^.|.$", "");
        } else if (type.equals("NUMBER")) {
            return Integer.parseInt(value);
        } else if (type.equals("BOOLEAN")) {
            return Boolean.parseBoolean(value);
        } else {
            throw new KevaSQLException("unknown type: " + type);
        }
    }

    public List<List<Object>> select(Statement stmt) {
        Select selectStatement = (Select) stmt;
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
        String tableName = tableList.get(0);
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist!");
        }
        List<String> columns = new ArrayList<>();
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            columns.add(selectItem.toString());
        }
        List<List<Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : tableData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(tableName + ":")) {
                List<Object> value = entry.getValue();
                List<Object> row = new ArrayList<>();
                for (String column : columns) {
                    if (column.equals("*")) {
                        result.addAll(Collections.singleton(value));
                        break;
                    } else {
                        int index = findColumn(column, columnDefinitions);
                        if (index == -1) {
                            throw new KevaSQLException("column " + column + " does not exist!");
                        }
                        row.add(value.get(index));
                    }
                }
                result.add(row);
            }
        }
        return result;
    }
}
