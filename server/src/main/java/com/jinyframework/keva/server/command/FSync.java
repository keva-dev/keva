package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.replication.master.ReplicationService;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FSync implements CommandHandler {
    private final StorageService storageService = ServiceInstance.getStorageService();
    private final ReplicationService replicationService = ServiceInstance.getReplicationService();

    public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Files.deleteIfExists(Path.of(zipFilePath));
        final Path p = Files.createFile(Path.of(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            final Path pp = Path.of(sourceDirPath);
            Files.walk(pp)
                 .filter(path -> {
                     try {
                         return !Files.isDirectory(path) && !Files.isSameFile(p, path);
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(path -> {
                     System.out.println(pp.relativize(path));
                     final ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                     try {
                         zs.putNextEntry(zipEntry);
                         Files.copy(path, zs);
                         zs.closeEntry();
                     } catch (IOException e) {
                         System.err.println(e);
                     }
                 });
        }
    }

    @Override
    public Object handle(List<String> args) {
        // send snapshot to replica
        try {
            final Path snapFolder = storageService.getSnapshotPath();
            final Path zipPath = snapFolder.resolve("data.zip");
            pack(snapFolder.toString(), zipPath.toString());
            // register replica and start buffering commands to forward
            log.info(String.valueOf(args));
            replicationService.addReplica(args.get(0) + ':' + args.get(1));
            log.info(zipPath.toString());
            return Base64.getEncoder().encodeToString(Files.readAllBytes(zipPath));
        } catch (IOException e) {
            log.error("FSYNC failed: ", e);
            return "null";
        }
    }
}
