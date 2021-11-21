package dev.keva.server.command.impl.kql.manager;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KqlManager {
    private final Map<String, List<KevaColumnDefinition>> metadata = new ConcurrentHashMap<>();
    private final Map<String, List<Object>> tableData = new ConcurrentHashMap<>();

    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    public void create(Statement stmt) {
        CreateTable createTable = (CreateTable) stmt;
        String tableName = createTable.getTable().getName();
        if (metadata.get(tableName) != null) {
            throw new KevaSQLException("table " + tableName + " already exists");
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

    public void insert(Statement stmt) {
        Insert insertStatement = (Insert) stmt;
        Table table = insertStatement.getTable();
        String tableName = table.getName();
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<Expression> insertValuesExpression = ((ExpressionList) insertStatement.getItemsList()).getExpressions();
        List<String> values = new ArrayList<>();
        for (Expression expression : insertValuesExpression) {
            values.add(expression.toString());
        }

        List<Object> result = new ArrayList<>(columnDefinitions.size());
        for (int i = 0; i < columnDefinitions.size(); i++) {
            result.add(null);
        }
        List<Column> insertColumns = insertStatement.getColumns();
        if (insertColumns != null) {
            for (Column column : insertColumns) {
                int index = KevaColumnFinder.findColumn(column.getColumnName(), columnDefinitions);
                if (index == -1) {
                    throw new KevaSQLException("column " + column + " does not exist");
                }
                String type = columnDefinitions.get(index).type;
                String value = values.get(index);
                result.set(index, addResult(type, value));
            }
        } else {
            for (int i = 0; i < columnDefinitions.size(); i++) {
                String type = columnDefinitions.get(i).type;
                String value = values.get(i);
                result.set(i, addResult(type, value));
            }
        }

        String uuid = UUID.randomUUID().toString();
        tableData.computeIfAbsent(tableName, k -> new ArrayList<>());
        tableData.put(tableName + ":" + uuid, result);
    }

    private Object addResult(String type, String value) {
        value = KevaSQLStringUtil.escape(value);
        if (value.toLowerCase().equals("null")) {
            return null;
        }
        if (type.equals("CHAR") || type.equals("VARCHAR") || type.equals("TEXT")) {
            return value;
        } else if (type.equals("INT") || type.equals("INTEGER")) {
            return Integer.parseInt(value);
        } else if (type.equals("BIGINT")) {
            return Long.parseLong(value);
        } else if (type.equals("DOUBLE")) {
            return Double.parseDouble(value);
        } else if (type.equals("FLOAT")) {
            return Float.parseFloat(value);
        } else if (type.equals("BOOL") || type.equals("BOOLEAN")) {
            return Boolean.parseBoolean(value);
        } else {
            throw new KevaSQLException("unknown type: " + type);
        }
    }

    public int update(Statement stmt) {
        Update updateStatement = (Update) stmt;
        Expression where = updateStatement.getWhere();
        if (where == null) {
            return 0;
        }
        String tableName = updateStatement.getTable().getName();
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<List<Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : tableData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(tableName + ":")) {
                List<Object> value = entry.getValue();
                result.addAll(Collections.singleton(value));
            }
        }
        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        where.accept(kqlExpressionVisitor);
        List<List<Object>> toBeUpdated = kqlExpressionVisitor.getTemp();
        int count = 0;
        for (Map.Entry<String, List<Object>> entry : tableData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(tableName + ":")) {
                List<Object> value = entry.getValue();
                if (toBeUpdated.contains(value)) {
                    // Update
                    List<Column> updateColumns = updateStatement.getColumns();
                    for (int i = 0; i < updateColumns.size(); i++) {
                        int index = KevaColumnFinder.findColumn(updateColumns.get(i).getColumnName(), columnDefinitions);
                        if (index == -1) {
                            throw new KevaSQLException("column " + updateColumns.get(i).getColumnName() + " does not exist");
                        }
                        String type = columnDefinitions.get(index).type;
                        String updatedValue = updateStatement.getExpressions().get(i).toString();
                        value.set(index, addResult(type, updatedValue));
                    }
                    count++;
                }
            }
        }
        return count;
    }

    public int delete(Statement stmt) {
        Delete deleteStatement = (Delete) stmt;
        Expression where = deleteStatement.getWhere();
        if (where == null) {
            return 0;
        }
        String tableName = deleteStatement.getTable().getName();
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<List<Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : tableData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(tableName + ":")) {
                List<Object> value = entry.getValue();
                result.addAll(Collections.singleton(value));
            }
        }
        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        where.accept(kqlExpressionVisitor);
        List<List<Object>> toBeDeleted = kqlExpressionVisitor.getTemp();
        int count = 0;
        for (Map.Entry<String, List<Object>> entry : tableData.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(tableName + ":")) {
                List<Object> value = entry.getValue();
                if (toBeDeleted.contains(value)) {
                    tableData.remove(key);
                    count++;
                }
            }
        }
        return count;
    }

    public List<List<Object>> select(Statement stmt) {
        Select selectStatement = (Select) stmt;
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
        String tableName = tableList.get(0);
        List<KevaColumnDefinition> columnDefinitions = metadata.get(tableName);
        if (columnDefinitions == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
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
                List<Object> row = null;
                for (String column : columns) {
                    if (column.equals("*") || column.equals("COUNT(*)") ||
                            column.startsWith("COUNT(") || column.startsWith("SUM(") || column.startsWith("AVG(") ||
                            column.startsWith("MIN(") || column.startsWith("MAX(")) {
                        result.addAll(Collections.singleton(value));
                        break;
                    } else {
                        int index = KevaColumnFinder.findColumn(column, columnDefinitions);
                        if (index == -1) {
                            throw new KevaSQLException("column " + column + " does not exist");
                        }
                        if (row == null) {
                            row = new ArrayList<>();
                        }
                        row.add(value.get(index));
                    }
                }
                if (row != null) {
                    result.add(row);
                }
            }
        }

        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(kqlExpressionVisitor);
        }
        if (columns.get(0).equals("COUNT(*)")) {
            return Collections.singletonList(Collections.singletonList(kqlExpressionVisitor.getTemp().size()));
        } else if (columns.get(0).startsWith("COUNT(") ||  columns.get(0).startsWith("AVG(") || columns.get(0).startsWith("SUM(")
                || columns.get(0).startsWith("MIN(") || columns.get(0).startsWith("MAX(")) {
            String columnInBracket = columns.get(0).substring(columns.get(0).indexOf("(")+1, columns.get(0).indexOf(")"));
            int index = KevaColumnFinder.findColumn(columnInBracket, columnDefinitions);
            if (index == -1) {
                throw new KevaSQLException("column " + columnInBracket + " does not exist");
            }
            if (columns.get(0).startsWith("COUNT(")) {
                int count = (int) kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null).count();
                return KevaSQLStringUtil.singleSelectResponse(count);
            } else if (columns.get(0).startsWith("MIN(")) {
                Optional<Double> minOptional = kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null)
                        .map(row -> row.get(index).toString())
                        .map(Double::parseDouble)
                        .min(Double::compare);
                return minOptional.map(KevaSQLStringUtil::singleSelectResponse)
                        .orElseGet(() -> KevaSQLStringUtil.singleSelectResponse(null));
            } else if (columns.get(0).startsWith("MAX(")) {
                Optional<Double> maxOptional = kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null)
                        .map(row -> row.get(index).toString())
                        .map(Double::parseDouble)
                        .max(Double::compare);
                return maxOptional.map(KevaSQLStringUtil::singleSelectResponse)
                        .orElseGet(() -> KevaSQLStringUtil.singleSelectResponse(null));
            }
            Double sum = 0.0D;
            int count = 0;
            List<List<Object>> temp = kqlExpressionVisitor.getTemp();
            for (List<Object> row : temp) {
                if (row.get(index) != null) {
                    sum += (Double) row.get(index);
                    count++;
                }
            }
            if (columns.get(0).startsWith("AVG(")) {
                return KevaSQLStringUtil.singleSelectResponse(sum/count);
            }
            return KevaSQLStringUtil.singleSelectResponse(sum);
        }
        return kqlExpressionVisitor.getTemp();
    }
}
