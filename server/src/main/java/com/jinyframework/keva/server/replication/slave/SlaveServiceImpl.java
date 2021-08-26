package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SlaveServiceImpl implements SlaveService {
    private final ReplicationService replicationService;
    private String masterId;

    public SlaveServiceImpl(ReplicationService replicationService) {
        this.replicationService = replicationService;
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
        final CompletableFuture<Object> res = syncClient.sendSync(config.getHostname(), config.getPort());
        final String[] respContent = res.get().toString().split(" ");
        if ("F".equals(respContent[0])) {
            log.info("Performing full synchronization");
            final byte[] snapContent = Base64.getDecoder().decode(respContent[3]);
            final Path kdbFile = Path.of(config.getSnapshotLocation(), "dump.kdb");
            Files.createDirectories(Path.of(config.getSnapshotLocation()));
            Files.write(kdbFile, snapContent);
            masterId = respContent[2];
            log.info("Finished writing snapshot file");
            // restart storage service to apply changes
        } else {
            throw new Exception("Failed to full sync with master");
        }
    }
}
