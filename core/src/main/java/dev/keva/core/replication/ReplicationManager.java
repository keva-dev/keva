package dev.keva.core.replication;

import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.core.command.mapping.CommandWrapper;
import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.SafeEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class ReplicationManager {

    private final KevaConfig kevaConfig;
    private final ReplicationBuffer repBuffer;
    private final KevaDatabase kevaDatabase;
    private final CommandMapper commandMapper;
    private final String snapshotFilePath;

    @Autowired
    public ReplicationManager(KevaConfig kevaConfig, ReplicationBuffer replicationBuffer, KevaDatabase kevaDatabase, CommandMapper commandMapper) {
        this.kevaConfig = kevaConfig;
        this.repBuffer = replicationBuffer;
        this.kevaDatabase = kevaDatabase;
        this.commandMapper = commandMapper;
        this.snapshotFilePath = kevaConfig.getWorkDirectory() + "/temp.kdb";
    }

    public void init() {
        // both slave and master need replication buffer initialized
        repBuffer.init();
        if (kevaConfig.getReplicaOf() == null || kevaConfig.getReplicaOf().isEmpty()) {
            return;
        }
        Jedis jedis = createJedisClient(kevaConfig.getReplicaOf());
        String response;

        // REPLCONF <option> <value> <option> <value> ...
        StringBuilder replConfArgBuilder = new StringBuilder();
        replConfArgBuilder.append(ReplConstants.IP_ADDRESS).append(" ")
            .append(kevaConfig.getHostname()).append(" ")
            .append(ReplConstants.LISTENING_PORT).append(" ")
            .append(kevaConfig.getPort()).append(" ");
        byte[] replConfResponse = (byte[]) jedis.sendBlockingCommand(ReplicationCommand.REPLCONF, replConfArgBuilder.toString());
        response = SafeEncoder.encode(replConfResponse);
        if (response.startsWith("OK")) {
            // create heartbeat cron here
        }

        // PSYNC replicationId startingOffset
        byte[] rawResponse = (byte[]) jedis.sendBlockingCommand(ReplicationCommand.PSYNC, String.valueOf(repBuffer.getReplicationId()),
            String.valueOf(repBuffer.getStartingOffset()));
        response = SafeEncoder.encode(rawResponse);
        if (response.startsWith("FULLRESYNC")) {
            // parse the new replication id and offset
            String[] fullResyncRes = response.split(" ");
            String masterRepId = fullResyncRes[1];
            String masterOffset = fullResyncRes[2];
            // somehow receive snapshot data from master
            downloadSnapshotFile(jedis.getClient().getBinaryBulkReply());
            // reload buffer + storage
            reloadBufferAndStorage(masterRepId, masterOffset);
            // continue to receive commands from master's replication buffer
            replicateBufferedCommands(jedis.getClient().getMultiBulkReply());
        } else if (response.equalsIgnoreCase("CONTINUE")) {
            // continue to receive commands from master's replication buffer
            replicateBufferedCommands(jedis.getClient().getMultiBulkReply());
        }

        // send REPLCONF ACK to tell master start forwarding command
        replConfResponse = (byte[]) jedis.sendBlockingCommand(ReplicationCommand.REPLCONF, "ACK " + repBuffer.getCurrentOffset());
        response = SafeEncoder.encode(replConfResponse);
        if (response.startsWith("OK")) {
            log.info("Partial sync process successful");
        } else {
            log.info("Failed to ACK master");
        }
        log.info("Partial sync process completed");
    }

    private void replicateBufferedCommands(List<String> multiBulkReply) {
        for (String cmd : multiBulkReply) {
            String[] cmdArgs = cmd.split(" ");
            byte[][] objects = new byte[cmdArgs.length][];
            for (int i = 0; i < cmdArgs.length; i++) {
                String token = cmdArgs[i];
                objects[i] = token.getBytes(StandardCharsets.UTF_8);
            }
            Command command = Command.newInstance(objects, false);
            byte[] name = command.getName();
            CommandWrapper commandWrapper = commandMapper.getMethods().get(new BytesKey(name));
            if (commandWrapper != null) {
                try {
                    commandWrapper.execute(null, command);
                } catch (InterruptedException e) {
                    log.error("Failed to replicate buffered commands");
                }
            }

        }
    }

    private void reloadBufferAndStorage(String masterRepId, String masterOffset) {
        // load snapshot into db
        kevaDatabase.loadFromSnapshot(snapshotFilePath);
        // rebase replication buffer
        repBuffer.rebase(Long.parseLong(masterRepId), Long.parseLong(masterOffset));
    }

    @SneakyThrows
    private void downloadSnapshotFile(byte[] binaryBulkReply) {
        Path snapshot = Paths.get(snapshotFilePath);
        Files.write(snapshot, binaryBulkReply);
    }

    private Jedis createJedisClient(String replicaOf) {
        String[] hostAndPort = replicaOf.split(" ");
        String masterHost = hostAndPort[0];
        String masterPort = hostAndPort[1];
        return new Jedis(masterHost, Integer.parseInt(masterPort));
    }

}
