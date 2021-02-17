package com.jinyframework.keva.server.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SnapshotConfig {
    private final String snapshotInterval;
    private final String backupPath;
    private final String recoveryPath;
}
