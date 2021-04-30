package com.jinyframework.keva.performance;

import com.jinyframework.keva.store.NoHeapStore;
import com.jinyframework.keva.store.NoHeapStoreManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerformanceTest {
    public final static int THREAD_POOL_SIZE = 1;

    public static void main(String[] args) throws InterruptedException {
        val manager = new NoHeapStoreManager();
        manager.createStore("Performance-Test", NoHeapStore.Storage.IN_MEMORY, 1843);
        val noHeapStore = manager.getStore("Performance-Test");
        ArrayList<String> list = new ArrayList<>();
        for (int i1 = 1; i1 < 1_000_000; i1++) {
            list.add(String.valueOf(i1));
        }
        Collections.shuffle(list);

        performTest(() -> {
            for (val x : list) {
                // Put value
                noHeapStore.putString(x, x);
                // Get value
                // noHeapStore.getString(x);
            }
        }, noHeapStore.getClass().getName());

        ConcurrentHashMap<String, String> hashMap = new ConcurrentHashMap<>();
        performTest(() -> {
            for (val x : list) {
                // Put value
                hashMap.put(x, x);
                // Get value
                // hashMap.get(x);
            }
        }, hashMap.getClass().getName());
    }

    // Currently single thread test
    public static void performTest(FuncToTest funcToTest, String testName) throws InterruptedException {
        log.info("Thread usage: " + THREAD_POOL_SIZE);

        log.info("Test started for: " + testName);

        long averageTime = 0;
        for (int i = 0; i < 10; i++) {
            long startTime = System.nanoTime();
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            for (int j = 0; j < THREAD_POOL_SIZE; j++) {
                executorService.execute(funcToTest::testFunc);
            }

            executorService.shutdown();
            val term = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            if (!term) {
                log.error("Cannot terminate thread pool!");
            }

            long endTime = System.nanoTime();
            long totalTime = (endTime - startTime) / 1000000L;
            averageTime += totalTime;
            log.info("1M entries added/retrieved in " + totalTime + " ms");
        }

        log.info("For " + testName + " the average time is " + averageTime / 10 + " ms\n");
    }

    @FunctionalInterface
    interface FuncToTest {
        void testFunc();
    }
}
