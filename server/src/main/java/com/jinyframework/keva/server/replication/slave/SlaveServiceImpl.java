package com.jinyframework.keva.server.replication.slave;

import com.jinyframework.keva.server.config.ConfigHolder;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class SlaveServiceImpl implements SlaveService {
    private static InetSocketAddress parseMaster(String addr) {
        final String[] s = addr.split(":");
        final String host = s[0];
        final int port = Integer.parseInt(s[1]);
        return new InetSocketAddress(host, port);
    }

    @SneakyThrows
    private static void unzip(String dest, String src) {
        final File fileZip = Path.of(src).toFile();
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            final Path destFile = Files.createFile(Path.of(dest, zipEntry.getName()));
            Files.write(destFile, zis.readAllBytes());
            zipEntry = zis.getNextEntry();
        }
    }

    @Override
    public void start(ConfigHolder config) throws InterruptedException, IOException, ExecutionException {
        final InetSocketAddress addr = parseMaster(config.getReplicaOf());
        final SyncClient syncClient = new SyncClient(addr.getHostName(), addr.getPort());
        boolean success = syncClient.connect();
        while (!success) {
            success = syncClient.connect();
        }
        final Promise<Object> res = syncClient.fullSync(config.getHostname(), config.getPort());
        final byte[] snapContent;
        snapContent = Base64.getDecoder().decode((String) res.get());
        final Path zipFile = Path.of(config.getSnapshotLocation(), "data.zip");
        Files.createDirectories(Path.of(config.getSnapshotLocation()));
        Files.write(zipFile, snapContent);
        unzip(config.getSnapshotLocation(), zipFile.toString());
        log.info("Finished writing snapshot file");
    }
}
