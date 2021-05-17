package com.jinyframework.keva.store;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoreTest {
    private static final String storeName = "junit-test";
    private static NoHeapStoreManager manager = null;
    private static NoHeapStore noHeapStore = null;

    @BeforeAll
    static void initStore() {
        manager = new NoHeapStoreManager();
        manager.createStore(storeName, NoHeapStore.Storage.IN_MEMORY, 1843);
        noHeapStore = manager.getStore(storeName);
    }

    @Test
    void getSet() {
        val setAbc = noHeapStore.putString("abc", "123");
        val getAbc = noHeapStore.getString("abc");
        val getNull = noHeapStore.getString("notExistedKey");
        assertTrue(setAbc);
        assertTrue("123".contentEquals(getAbc));
        assertTrue("null".contentEquals(getNull));
    }

    @AfterAll
    static void clean() {
        manager.deleteStore(storeName);
    }
}
