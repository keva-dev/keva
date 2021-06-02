package com.jinyframework.keva.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;

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
    void getSetString() {
        String testName = "getSetString";
        val setAbc = noHeapStore.putString(testName + "abc", "123");
        val getAbc = noHeapStore.getString(testName + "abc");
        val getNull = noHeapStore.getString(testName + "notExisted");
        assertTrue(setAbc);
        assertEquals("123", getAbc);
        assertNull(getNull);
    }

    @Test
    void reAssignString() {
        String testName = "reAssignString";
        noHeapStore.putString(testName + "key1", "val1");
        assertEquals("val1", noHeapStore.getString(testName + "key1"));
        noHeapStore.putString(testName + "key1", "val2");
        assertEquals("val2", noHeapStore.getString(testName + "key1"));
        noHeapStore.remove(testName + "key1");
        assertNull(noHeapStore.getString(testName + "key1"));
    }

    @Test
    void getSetInteger() {
        String testName = "getSetInteger";
        val setAbc = noHeapStore.putInteger(testName + "abc", 123);
        val getAbc = noHeapStore.getInteger(testName + "abc");
        val getNull = noHeapStore.getInteger(testName + "notExisted");
        assertTrue(setAbc);
        assertEquals(123, getAbc);
        assertNull(getNull);
    }

    @Test
    void reAssignInteger() {
        String testName = "reAssignString";
        noHeapStore.putInteger(testName + "key1", 1);
        assertEquals(1, noHeapStore.getInteger(testName + "key1"));
        noHeapStore.putInteger(testName + "key1", 2);
        assertEquals(2, noHeapStore.getInteger(testName + "key1"));
        noHeapStore.remove(testName + "key1");
        assertNull(noHeapStore.getInteger(testName + "key1"));
    }

    @Test
    void getSetObject() {
        String testName = "getSetObject";
        val testObj = TestObject.builder()
                                .email("keva@mail")
                                .name("keva")
                                .build();
        noHeapStore.putObject(testName + "key", testObj);
        val got = (TestObject) noHeapStore.getObject(testName + "key");
        assertEquals("keva@mail", got.email);

        val testObj2 = TestObject.builder()
                                .email("keva2@mail")
                                .name("keva2")
                                .build();
        noHeapStore.putObject(testName + "key", testObj2);
        val got2 = (TestObject) noHeapStore.getObject(testName + "key");
        assertEquals("keva2@mail", got2.email);

        noHeapStore.remove("key");
        assertNull(noHeapStore.getInteger(testName + "key"));

        assertEquals("keva", got.name);
    }

    @Builder
    static class TestObject implements Serializable {
        String email;
        String name;
    }

    // TODO: Add collision cases

    @AfterAll
    static void clean() {
        manager.deleteStore(storeName);
    }
}
