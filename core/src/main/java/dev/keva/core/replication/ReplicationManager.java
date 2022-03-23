package dev.keva.core.replication;

import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.SafeEncoder;

@Component
@Slf4j
public class ReplicationManager {

    private final KevaConfig kevaConfig;
    private final ReplicationBuffer repBuffer;

    @Autowired
    public ReplicationManager(KevaConfig kevaConfig, ReplicationBuffer replicationBuffer) {
        this.kevaConfig = kevaConfig;
        this.repBuffer = replicationBuffer;
    }

    public void init() {
        // both slave and master need replication buffer initialized
        repBuffer.init();
        if (kevaConfig.getReplicaOf() == null || kevaConfig.getReplicaOf().isEmpty()) {
            return;
        }
        Jedis jedis = createJedisClient(kevaConfig.getReplicaOf());
        byte[] rawResponse = (byte[]) jedis.sendBlockingCommand(ReplicationCommand.PSYNC, String.valueOf(repBuffer.getReplicationId()),
            String.valueOf(repBuffer.getStartingOffset()));
        String response = SafeEncoder.encode(rawResponse);
        if (response.startsWith("FULLRESYNC")) {
            // parse the new replication id and offset
            // somehow receive snapshot data from master
            // reload buffer + storage
            // continue to receive commands from master's replication buffer
        } else if (response.equalsIgnoreCase("CONTINUE")) {
            // continue to receive commands from master's replication buffer
        }
    }

    private Jedis createJedisClient(String replicaOf) {
        String[] hostAndPort = replicaOf.split(" ");
        String masterHost = hostAndPort[0];
        String masterPort = hostAndPort[1];
        return new Jedis(masterHost, Integer.parseInt(masterPort));
    }

}
