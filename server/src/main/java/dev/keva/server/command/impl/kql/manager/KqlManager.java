package dev.keva.server.command.impl.kql.manager;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.store.KevaDatabase;
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
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KqlManager {
    private final KevaDatabase database;

    @Autowired
    public KqlManager(KevaDatabase database) {
        this.database = database;
    }

    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    public Object sqlGet(String key) {
        byte[] got = database.get(key.getBytes());
        if (got == null) {
            return null;
        }
        return SerializationUtils.deserialize(got);
    }

    public void sqlPut(String key, Object object) {
        database.put(key.getBytes(), SerializationUtils.serialize((Serializable) object));
    }

    public void sqlRemove(String key) {
        database.remove(key.getBytes());
    }

    public boolean sqlContainsKey(String key) {
        return sqlGet(key) != null;
    }

    public void create(Statement stmt) {
        CreateTable createTable = (CreateTable) stmt;
        String tableName = createTable.getTable().getName();
        if (sqlGet(tableName) != null) {
            throw new KevaSQLException("table " + tableName + " already exists");
        }
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
        List<KevaColumnDefinition> kevaColumns = new ArrayList<>();
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            KevaColumnDefinition kevaColumn = new KevaColumnDefinition
                    (columnDefinition.getColumnName().toLowerCase(),
                            columnDefinition.getColDataType().getDataType().toUpperCase());
            kevaColumns.add(kevaColumn);
        }
        KevaTable kevaTable = new KevaTable(kevaColumns);
        sqlPut(tableName, kevaTable);
    }

    @SuppressWarnings("unchecked")
    public void insert(Statement stmt) {
        Insert insertStatement = (Insert) stmt;
        Table table = insertStatement.getTable();
        String tableName = table.getName();
        KevaTable kevaTable = (KevaTable) sqlGet(tableName);
        if (kevaTable == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<KevaColumnDefinition> columnDefinitions = kevaTable.getColumns();
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
                result.set(index, KevaSQLConvertUtil.convertToRowData(type, value));
            }
        } else {
            for (int i = 0; i < columnDefinitions.size(); i++) {
                String type = columnDefinitions.get(i).type;
                String value = values.get(i);
                result.set(i, KevaSQLConvertUtil.convertToRowData(type, value));
            }
        }
        String id = Long.toString(kevaTable.getIncrement());
        sqlPut(tableName + ":" + id, result);
        kevaTable.increment();
        sqlPut(tableName, kevaTable);
    }

    @SuppressWarnings("unchecked")
    public int update(Statement stmt) {
        Update updateStatement = (Update) stmt;
        Expression where = updateStatement.getWhere();
        if (where == null) {
            return 0;
        }
        String tableName = updateStatement.getTable().getName();
        KevaTable kevaTable = (KevaTable) sqlGet(tableName);
        if (kevaTable == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<KevaColumnDefinition> columnDefinitions = kevaTable.getColumns();
        List<List<Object>> result = new ArrayList<>();
        for (int i = 0; i < kevaTable.getIncrement(); i++) {
            String key = tableName + ":" + i;
            if (sqlContainsKey(key)) {
                List<Object> value = (List<Object>) sqlGet(key);
                result.addAll(Collections.singleton(value));
            }
        }
        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        where.accept(kqlExpressionVisitor);
        List<List<Object>> toBeUpdated = kqlExpressionVisitor.getTemp();
        int count = 0;
        for (int i = 0; i < kevaTable.getIncrement(); i++) {
            String key = tableName + ":" + i;
            if (sqlContainsKey(key)) {
                List<Object> value = (List<Object>) sqlGet(key);
                if (value != null && toBeUpdated.contains(value)) {
                    // Update
                    List<Column> updateColumns = updateStatement.getColumns();
                    for (int j = 0; j < updateColumns.size(); j++) {
                        int index = KevaColumnFinder.findColumn(updateColumns.get(j).getColumnName(), columnDefinitions);
                        if (index == -1) {
                            throw new KevaSQLException("column " + updateColumns.get(j).getColumnName() + " does not exist");
                        }
                        String type = columnDefinitions.get(index).type;
                        String updatedValue = updateStatement.getExpressions().get(j).toString();
                        value.set(index, KevaSQLConvertUtil.convertToRowData(type, updatedValue));
                    }
                    sqlPut(key, value);
                    count++;
                }
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public int delete(Statement stmt) {
        Delete deleteStatement = (Delete) stmt;
        Expression where = deleteStatement.getWhere();
        if (where == null) {
            return 0;
        }
        String tableName = deleteStatement.getTable().getName();
        KevaTable kevaTable = (KevaTable) sqlGet(tableName);
        if (kevaTable == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<KevaColumnDefinition> columnDefinitions = kevaTable.getColumns();
        List<List<Object>> result = new ArrayList<>();
        for (int i = 0; i < kevaTable.getIncrement(); i++) {
            String key = tableName + ":" + i;
            if (sqlContainsKey(key)) {
                List<Object> value = (List<Object>) sqlGet(key);
                result.addAll(Collections.singleton(value));
            }
        }
        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        where.accept(kqlExpressionVisitor);
        List<List<Object>> toBeDeleted = kqlExpressionVisitor.getTemp();
        int count = 0;
        for (int i = 0; i < kevaTable.getIncrement(); i++) {
            String key = tableName + ":" + i;
            if (sqlContainsKey(key)) {
                List<Object> value = (List<Object>) sqlGet(key);
                if (toBeDeleted.contains(value)) {
                    sqlRemove(key);
                    count++;
                }
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public List<List<Object>> select(Statement stmt) {
        Select selectStatement = (Select) stmt;
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
        String tableName = tableList.get(0);
        KevaTable kevaTable = (KevaTable) sqlGet(tableName);
        if (kevaTable == null) {
            throw new KevaSQLException("table " + tableName + " does not exist");
        }
        List<KevaColumnDefinition> columnDefinitions = kevaTable.getColumns();
        List<String> columns = new ArrayList<>();
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            columns.add(selectItem.toString());
        }
        List<List<Object>> result = new ArrayList<>();
        for (int i = 0; i < kevaTable.getIncrement(); i++) {
            String key = tableName + ":" + i;
            if (sqlContainsKey(key)) {
                List<Object> value = (List<Object>) sqlGet(key);
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

        List<List<Object>> proceededResult = selectProcess(plainSelect, result, columns, columnDefinitions);
        return selectPostProcess(plainSelect, proceededResult, columns, columnDefinitions);
    }

    private List<List<Object>> selectProcess(PlainSelect plainSelect, List<List<Object>> result,
                                             List<String> columns, List<KevaColumnDefinition> columnDefinitions) {
        KqlExpressionVisitor kqlExpressionVisitor = new KqlExpressionVisitor(result, columnDefinitions);
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(kqlExpressionVisitor);
        }
        if (columns.get(0).equals("COUNT(*)")) {
            return Collections.singletonList(Collections.singletonList(kqlExpressionVisitor.getTemp().size()));
        } else if (columns.get(0).startsWith("COUNT(") || columns.get(0).startsWith("AVG(") || columns.get(0).startsWith("SUM(")
                || columns.get(0).startsWith("MIN(") || columns.get(0).startsWith("MAX(")) {
            String columnInBracket = columns.get(0).substring(columns.get(0).indexOf("(") + 1, columns.get(0).indexOf(")"));
            int index = KevaColumnFinder.findColumn(columnInBracket, columnDefinitions);
            if (index == -1) {
                throw new KevaSQLException("column " + columnInBracket + " does not exist");
            }
            if (columns.get(0).startsWith("COUNT(")) {
                int count = (int) kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null).count();
                return KevaSQLResponseUtil.singleSelectResponse(count);
            } else if (columns.get(0).startsWith("MIN(")) {
                Optional<Double> minOptional = kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null)
                        .map(row -> row.get(index).toString())
                        .map(Double::parseDouble)
                        .min(Double::compare);
                return minOptional.map(KevaSQLResponseUtil::singleSelectResponse)
                        .orElseGet(() -> KevaSQLResponseUtil.singleSelectResponse(null));
            } else if (columns.get(0).startsWith("MAX(")) {
                Optional<Double> maxOptional = kqlExpressionVisitor.getTemp().stream().filter(row -> row.get(index) != null)
                        .map(row -> row.get(index).toString())
                        .map(Double::parseDouble)
                        .max(Double::compare);
                return maxOptional.map(KevaSQLResponseUtil::singleSelectResponse)
                        .orElseGet(() -> KevaSQLResponseUtil.singleSelectResponse(null));
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
                return KevaSQLResponseUtil.singleSelectResponse(sum / count);
            }
            return KevaSQLResponseUtil.singleSelectResponse(sum);
        }
        return kqlExpressionVisitor.getTemp();
    }

    private List<List<Object>> selectPostProcess(PlainSelect plainSelect, List<List<Object>> result,
                                                 List<String> columns, List<KevaColumnDefinition> columnDefinitions) {
        List<List<Object>> postProcessedResult = result;
        if (plainSelect.getOrderByElements() != null) {
            Stream<List<Object>> sortedResultStream;
            for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
                String column = orderByElement.getExpression().toString();
                int index = KevaColumnFinder.findColumn(column, columnDefinitions);
                if (index == -1) {
                    throw new KevaSQLException("column " + column + " does not exist");
                }
                String type = columnDefinitions.get(index).type;
                sortedResultStream = postProcessedResult.stream()
                        .sorted((p1, p2) -> {
                            if (p1.get(index) == null && p2.get(index) == null) {
                                return 0;
                            } else if (p1.get(index) == null) {
                                return -1;
                            } else if (p2.get(index) == null) {
                                return 1;
                            } else if (type.equals("TEXT") || type.equals("VARCHAR") || type.equals("CHAR")) {
                                return p1.get(index).toString().compareTo(p2.get(index).toString());
                            } else if (type.equals("INTEGER") || type.equals("INT")) {
                                return ((Integer) p1.get(index)).compareTo((Integer) p2.get(index));
                            } else if (type.equals("DOUBLE") || type.equals("FLOAT")) {
                                return ((Double) p1.get(index)).compareTo((Double) p2.get(index));
                            } else if (type.equals("BOOLEAN")) {
                                return ((Boolean) p1.get(index)).compareTo((Boolean) p2.get(index));
                            } else {
                                return 0;
                            }
                        });
                if (orderByElement.isAsc()) {
                    postProcessedResult = sortedResultStream.collect(Collectors.toList());
                } else {
                    postProcessedResult = sortedResultStream.collect(Collectors.toList());
                    Collections.reverse(postProcessedResult);
                }
            }
        }
        if (plainSelect.getOffset() != null) {
            long offset = plainSelect.getOffset().getOffset();
            postProcessedResult = postProcessedResult.stream().skip(offset).collect(Collectors.toList());

        }
        if (plainSelect.getLimit() != null) {
            Stream<List<Object>> limitedResultStream = postProcessedResult.stream();
            if (plainSelect.getLimit().getOffset() != null) {
                long offset = Long.parseLong(plainSelect.getLimit().getOffset().toString());
                limitedResultStream = limitedResultStream.skip(offset);
            }
            if (plainSelect.getLimit().getRowCount() != null) {
                long rowCount = Long.parseLong(plainSelect.getLimit().getRowCount().toString());
                limitedResultStream = limitedResultStream.limit(rowCount);
            }
            postProcessedResult = limitedResultStream.collect(Collectors.toList());
        }
        return postProcessedResult;
    }
}
