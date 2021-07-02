package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.config.ConfigHolder;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

@Slf4j
public class SlaveServiceImpl implements SlaveService {
    private static InetSocketAddress parseMaster(String addr) {
        final String[] s = addr.split(":");
        final String host = s[0];
        final int port = Integer.parseInt(s[1]);
        return new InetSocketAddress(host, port);
    }

    @Override
    public void start(ConfigHolder config) throws InterruptedException, IOException, ExecutionException {
        final InetSocketAddress addr = parseMaster(config.getReplicaOf());
        final SyncClient syncClient = new SyncClient(addr.getHostName(), addr.getPort());
        boolean success = syncClient.connect();
        while (!success) {
            success = syncClient.connect();
        }
        final Promise<Object> res = syncClient.fullSync(config.getHostname(),config.getPort());
        final byte[] snapContent;
        snapContent = (byte[]) res.get();
        final Path kevaData = Path.of(config.getSnapshotLocation(), "KevaData");
        Files.createDirectories(Path.of(config.getSnapshotLocation()));
        Files.write(kevaData, snapContent);
        log.info("Finished writing snapshot file");
    }
}
