package dev.keva.core.replication;

import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReplicationManager {

    private final KevaConfig kevaConfig;
    private final ReplicationBuffer replicationBuffer;

    @Autowired
    public ReplicationManager(KevaConfig kevaConfig, ReplicationBuffer replicationBuffer) {
        this.kevaConfig = kevaConfig;
        this.replicationBuffer = replicationBuffer;
    }

    public void init() {
        // both slave and master need replication buffer initialized
        replicationBuffer.init();
        if (kevaConfig.getReplicaOf() == null || kevaConfig.getReplicaOf().isEmpty()) {
            return;
        }
    }

}
