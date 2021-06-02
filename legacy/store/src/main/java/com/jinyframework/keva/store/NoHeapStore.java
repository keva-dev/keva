package com.jinyframework.keva.store;

public interface NoHeapStore {
    byte INACTIVE_RECORD = 0;
    byte ACTIVE_RECORD = 1;
    byte EMPTY_RECORD_TYPE = -1;
    byte OBJ_RECORD_TYPE = 1;
    byte TEXT_RECORD_TYPE = 2;
    byte LONG_RECORD_TYPE = 3;
    byte INT_RECORD_TYPE = 4;
    byte DOUBLE_RECORD_TYPE = 5;
    byte FLOAT_RECORD_TYPE = 6;
    byte SHORT_RECORD_TYPE = 7;
    byte CHAR_RECORD_TYPE = 8;
    byte BYTEARRAY_RECORD_TYPE = 9;

    // Get Journal stats
    long getRecordCount();

    long getEmptyCount();

    String getName();

    String getFolder();

    long getFileSize();

    boolean putInteger(String key, Integer val);

    Integer getInteger(String key);

    boolean putShort(String key, Short val);

    Short getShort(String key);

    boolean putLong(String key, Long val);

    Long getLong(String key);

    boolean putFloat(String key, Float val);

    Float getFloat(String key);

    boolean putDouble(String key, Double val);

    Double getDouble(String key);

    boolean putString(String key, String val);

    String getString(String key);

    boolean putObject(String key, Object msg);

    Object getObject(String key);

    boolean putChar(String key, char val);

    char getChar(String key);

    boolean remove(String key);

    Object iterateStart();

    Object iterateNext();

    void delete();

    enum Storage {
        IN_MEMORY,
        PERSISTED
    }
}
