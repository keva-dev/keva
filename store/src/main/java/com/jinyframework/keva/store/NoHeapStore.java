package com.jinyframework.keva.store;

public interface NoHeapStore {

    String getName();

    String getFolder();

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

    enum Storage {
        IN_MEMORY,
        PERSISTED
    }
}
