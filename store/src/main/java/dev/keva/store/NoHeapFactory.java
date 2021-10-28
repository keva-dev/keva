package dev.keva.store;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public final class NoHeapFactory {
    public static StorageService makeNoHeapDBStore(NoHeapConfig config) {
        return new NoHeapChronicleMapImpl(config);
    }
}
