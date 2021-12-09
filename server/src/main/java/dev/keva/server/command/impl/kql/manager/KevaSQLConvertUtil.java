package dev.keva.server.command.impl.kql.manager;

public class KevaSQLConvertUtil {
    public static Object convertToRowData(String type, String value) {
        value = KevaSQLStringUtil.escape(value);
        if (value.equalsIgnoreCase("null")) {
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
}
