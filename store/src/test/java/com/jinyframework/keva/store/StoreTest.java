package com.jinyframework.keva.store;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.*;

public class StoreTest {
    private static final String storeName = "junit-test";
    private static NoHeapStoreManager manager = null;
    private static NoHeapStore noHeapStore = null;

    @BeforeAll
    static void initStore() {
        try {
            manager = new NoHeapStoreManager();
            manager.createStore(storeName, NoHeapStore.Storage.IN_MEMORY, 8);
            noHeapStore = manager.getStore(storeName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getSet() {
        val setAbc = noHeapStore.putString("abc", "123");
        val getAbc = noHeapStore.getString("abc");
        val getNull = noHeapStore.getString("notExisted");
        assertTrue(setAbc);
        assertTrue("123".contentEquals(getAbc));
        assertNull(getNull);
    }

    @Test
    void reAssign() {
        noHeapStore.putString("key1", "val1");
        assertTrue("val1".contentEquals(noHeapStore.getString("key1")));
        noHeapStore.putString("key1", "val2");
        assertTrue("val2".contentEquals(noHeapStore.getString("key1")));
        noHeapStore.remove("key1");
        assertNull(noHeapStore.getString("key1"));
    }

    // TODO: Add collision cases

    @AfterAll
    static void clean() {
        manager.deleteStore(storeName);
    }
}
