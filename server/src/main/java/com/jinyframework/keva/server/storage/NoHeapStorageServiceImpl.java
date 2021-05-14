package com.jinyframework.keva.server.storage;

import com.jinyframework.keva.store.NoHeapStore;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NoHeapStorageServiceImpl implements StorageService {
    private static NoHeapStore store;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void setStore(NoHeapStore store) {
        NoHeapStorageServiceImpl.store = store;
    }

    @Override
    public boolean putString(String key, String val) {
        final Future<Boolean> res = executor.submit(() -> store.putString(key, val));
        try {
            return res.get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    @Override
    public String getString(String key) {
        final Future<String> res = executor.submit(() -> store.getString(key));
        try {
            return res.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    @Override
    public boolean remove(String key) {
        final Future<Boolean> res = executor.submit(() -> store.remove(key));
        try {
            return res.get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }
}