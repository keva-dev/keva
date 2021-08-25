package com.jinyframework.keva.store;

public interface NoHeapStore {

    String getName();

    String getFolder();

    boolean putString(String key, String val);

    String getString(String key);

    boolean remove(String key);
}
