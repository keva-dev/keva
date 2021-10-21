package dev.keva.store;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoHeapConfig {
    Boolean snapshotEnabled;
    Integer heapSize;
    String snapshotLocation;
}
