package dev.keva.server.replication.slave;

import dev.keva.server.command.setup.CommandService;
import dev.keva.server.config.ConfigHolder;
import dev.keva.server.core.WriteLog;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SlaveServiceImpl implements SlaveService {
    private final ScheduledExecutorService healthCheckerPool;
    private final WriteLog writeLog;
    private final CommandService commandService;
    private String masterId;

    public SlaveServiceImpl(ScheduledExecutorService healthCheckerPool, WriteLog writeLog, CommandService commandService) {
        this.healthCheckerPool = healthCheckerPool;
        this.writeLog = writeLog;
        this.commandService = commandService;
    }

    private static InetSocketAddress parseMaster(String addr) {
        final String[] s = addr.split(":");
        final String host = s[0];
        final int port = Integer.parseInt(s[1]);
        return new InetSocketAddress(host, port);
    }

    @Override
    public void start(ConfigHolder config) throws Exception {
        final InetSocketAddress addr = parseMaster(config.getReplicaOf());
        final SyncClient syncClient = new SyncClient(addr.getHostName(), addr.getPort());
        boolean success = syncClient.connect();
        while (!success) {
            success = syncClient.connect();
        }
        String slaveHostName = config.getHostname();
        Integer slavePort = config.getPort();
        syncTask(config, writeLog, syncClient, slaveHostName, slavePort).run();
        healthCheckerPool.scheduleAtFixedRate(syncTask(config, writeLog, syncClient, slaveHostName, slavePort),
                5, 1, TimeUnit.SECONDS);
    }

    private Runnable syncTask(ConfigHolder config, WriteLog writeLog, SyncClient syncClient, String slaveHostName, Integer slavePort) {
        return () -> {
            try {
                CompletableFuture<Object> res = syncClient.sendSync(slaveHostName, slavePort, masterId, writeLog.getCurrentOffset());
                final String[] respContent = res.get().toString().split(" ");
                if ("F".equals(respContent[0])) {
                    doFullSync(config, respContent);
                } else if ("P".equals(respContent[0])) {
                    log.info("respContent: {}", Arrays.toString(respContent));
                    doPartialSync(respContent);
                } else {
                    throw new Exception("Failed to full sync with master");
                }
            } catch (Exception e) {
                log.error("Syncing with master error: ", e);
            }
        };
    }

    private void doPartialSync(String[] respContent) {
        log.info("Performing partial synchronization");
        if (respContent.length < 4) {
            return;
        }
        final String strListOfCommands = new String(Base64.getDecoder()
                .decode(respContent[3]), StandardCharsets.UTF_8);
        String[] listOfCommands = strListOfCommands.split("\n");
        for (String command : listOfCommands) {
            // TODO: update command here, now command is not string anymore
            // commandService.handleCommand(command);
        }
    }


    private void doFullSync(ConfigHolder config, String[] respContent) throws IOException {
        log.info("Performing full synchronization");
        final byte[] snapContent = Base64.getDecoder().decode(respContent[3]);
        final Path kdbFile = Path.of(config.getSnapshotLocation(), "dump.kdb");
        Files.createDirectories(Path.of(config.getSnapshotLocation()));
        Files.write(kdbFile, snapContent);
        masterId = respContent[1];
        log.info("Finished writing snapshot file");
        // restart storage service to apply changes
    }
}
