package com.jinyframework.keva.store;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Setter
@Slf4j
public final class NoHeapFactory {
    private NoHeapFactory() {
    }

    public static NoHeapStore makeNoHeapDBStore(NoHeapConfig config) {
        val db = new NoHeapStoreManager();
        val shouldPersist = config.getSnapshotEnabled();
        val heapSizeInMegabytes = config.getHeapSize();
        db.createStore("Keva",
                       shouldPersist ? NoHeapStore.Storage.PERSISTED : NoHeapStore.Storage.IN_MEMORY,
                       heapSizeInMegabytes, config.getSnapshotLocation());
        return db.getStore("Keva");
    }
}
