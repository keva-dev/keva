package dev.keva.server.command.impl.kql.manager;

import lombok.Getter;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KqlExpressionVisitor implements ExpressionVisitor {
    private List<List<Object>> result;
    private final List<KevaColumnDefinition> kevaColumns;

    @Getter
    private List<List<Object>> temp;

    public KqlExpressionVisitor(List<List<Object>> result, List<KevaColumnDefinition> kevaColumns) {
        this.result = result;
        this.kevaColumns = kevaColumns;
        this.temp = result;
    }

    @Override
    public void visit(BitwiseRightShift aThis) {

    }

    @Override
    public void visit(BitwiseLeftShift aThis) {

    }

    @Override
    public void visit(NullValue nullValue) {

    }

    @Override
    public void visit(Function function) {

    }

    @Override
    public void visit(SignedExpression signedExpression) {

    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {

    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {

    }

    @Override
    public void visit(DoubleValue doubleValue) {

    }

    @Override
    public void visit(LongValue longValue) {

    }

    @Override
    public void visit(HexValue hexValue) {

    }

    @Override
    public void visit(DateValue dateValue) {

    }

    @Override
    public void visit(TimeValue timeValue) {

    }

    @Override
    public void visit(TimestampValue timestampValue) {

    }

    @Override
    public void visit(Parenthesis parenthesis) {

    }

    @Override
    public void visit(StringValue stringValue) {

    }

    @Override
    public void visit(Addition addition) {

    }

    @Override
    public void visit(Division division) {

    }

    @Override
    public void visit(IntegerDivision division) {

    }

    @Override
    public void visit(Multiplication multiplication) {

    }

    @Override
    public void visit(Subtraction subtraction) {

    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.getLeftExpression().accept(this);
        result = temp;
        andExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.getLeftExpression().accept(this);
        List<List<Object>> combine = new ArrayList<>(temp);
        orExpression.getRightExpression().accept(this);
        combine.addAll(temp);
        temp = combine;
    }

    @Override
    public void visit(Between between) {

    }

    @Override
    public void visit(EqualsTo equalsTo) {
        String columnName = equalsTo.getLeftExpression().toString();
        int columnIndex = ColumnFinder.findColumn(columnName, kevaColumns);
        String type = kevaColumns.get(columnIndex).type;
        String valueStr = KevaSQLStrUtil.escape(equalsTo.getRightExpression().toString());
        if (type.equals("CHAR") || type.equals("VARCHAR") || type.equals("TEXT")) {
            temp = result.stream()
                    .filter(row -> row.get(columnIndex).equals(valueStr))
                    .collect(Collectors.toList());
        } else if (type.equals("INT") || type.equals("INTEGER")) {
            int value = Integer.parseInt(valueStr);
            temp = result.stream()
                    .filter(row -> (int) row.get(columnIndex) == value)
                    .collect(Collectors.toList());
        } else if (type.equals("FLOAT") || type.equals("DOUBLE")) {
            double value = Double.parseDouble(valueStr);
            temp = result.stream()
                    .filter(row -> (double) row.get(columnIndex) == value)
                    .collect(Collectors.toList());
        } else if (type.equals("BOOL") || type.equals("BOOLEAN")) {
            boolean value = Boolean.parseBoolean(valueStr);
            temp = result.stream()
                    .filter(row -> (boolean) row.get(columnIndex) == value)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void visit(GreaterThan greaterThan) {

    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {

    }

    @Override
    public void visit(InExpression inExpression) {

    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {

    }

    @Override
    public void visit(IsNullExpression isNullExpression) {

    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {

    }

    @Override
    public void visit(LikeExpression likeExpression) {

    }

    @Override
    public void visit(MinorThan minorThan) {

    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {

    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        String columnName = notEqualsTo.getLeftExpression().toString();
        int columnIndex = ColumnFinder.findColumn(columnName, kevaColumns);
        String type = kevaColumns.get(columnIndex).type;
        String valueStr = KevaSQLStrUtil.escape(notEqualsTo.getRightExpression().toString());
        if (type.equals("CHAR") || type.equals("VARCHAR") || type.equals("TEXT")) {
            temp = result.stream()
                    .filter(row -> !row.get(columnIndex).equals(valueStr))
                    .collect(Collectors.toList());
        } else if (type.equals("INT") || type.equals("INTEGER")) {
            int value = Integer.parseInt(valueStr);
            temp = result.stream()
                    .filter(row -> (int) row.get(columnIndex) != value)
                    .collect(Collectors.toList());
        } else if (type.equals("FLOAT") || type.equals("DOUBLE")) {
            double value = Double.parseDouble(valueStr);
            temp = result.stream()
                    .filter(row -> (double) row.get(columnIndex) != value)
                    .collect(Collectors.toList());
        } else if (type.equals("BOOL") || type.equals("BOOLEAN")) {
            boolean value = Boolean.parseBoolean(valueStr);
            temp = result.stream()
                    .filter(row -> (boolean) row.get(columnIndex) != value)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void visit(Column tableColumn) {

    }

    @Override
    public void visit(SubSelect subSelect) {

    }

    @Override
    public void visit(CaseExpression caseExpression) {

    }

    @Override
    public void visit(WhenClause whenClause) {

    }

    @Override
    public void visit(ExistsExpression existsExpression) {

    }

    @Override
    public void visit(AllComparisonExpression allComparisonExpression) {

    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {

    }

    @Override
    public void visit(Concat concat) {

    }

    @Override
    public void visit(Matches matches) {

    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {

    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {

    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {

    }

    @Override
    public void visit(CastExpression cast) {

    }

    @Override
    public void visit(Modulo modulo) {

    }

    @Override
    public void visit(AnalyticExpression aexpr) {

    }

    @Override
    public void visit(ExtractExpression eexpr) {

    }

    @Override
    public void visit(IntervalExpression iexpr) {

    }

    @Override
    public void visit(OracleHierarchicalExpression oexpr) {

    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {

    }

    @Override
    public void visit(JsonExpression jsonExpr) {

    }

    @Override
    public void visit(JsonOperator jsonExpr) {

    }

    @Override
    public void visit(RegExpMySQLOperator regExpMySQLOperator) {

    }

    @Override
    public void visit(UserVariable var) {

    }

    @Override
    public void visit(NumericBind bind) {

    }

    @Override
    public void visit(KeepExpression aexpr) {

    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) {

    }

    @Override
    public void visit(ValueListExpression valueList) {

    }

    @Override
    public void visit(RowConstructor rowConstructor) {

    }

    @Override
    public void visit(OracleHint hint) {

    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {

    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {

    }

    @Override
    public void visit(NotExpression aThis) {

    }

    @Override
    public void visit(NextValExpression aThis) {

    }

    @Override
    public void visit(CollateExpression aThis) {

    }

    @Override
    public void visit(SimilarToExpression aThis) {

    }

    @Override
    public void visit(ArrayExpression aThis) {

    }
}
