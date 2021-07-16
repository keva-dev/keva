package com.jinyframework.keva.server.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public final class ZipUtil {
    private ZipUtil() {
    }

    public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Files.deleteIfExists(Path.of(zipFilePath));
        final Path p = Files.createFile(Path.of(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            final Path pp = Path.of(sourceDirPath);
            try (Stream<Path> files = Files.walk(pp)) {
                files.filter(path -> {
                    try {
                        return !Files.isDirectory(path) && !Files.isSameFile(p, path);
                    } catch (IOException e) {
                        return false;
                    }
                })
                     .forEach(path -> {
                         log.info("Zipping: {}", pp.relativize(path));
                         final ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                         try {
                             zs.putNextEntry(zipEntry);
                             Files.copy(path, zs);
                             zs.closeEntry();
                         } catch (IOException e) {
                             log.error("Failed to zip file {}: ", path, e);
                         }
                     });
            }
        }
    }

    public static void unzip(String dest, String src) throws IOException {
        final int THRESHOLD_ENTRIES = 10000;
        final int THRESHOLD_SIZE = 1000000000; // 1 GB
        final double THRESHOLD_RATIO = 1500; // KevaData and KevaDataIndex seem to have compressed ration over 1000
        int totalSizeArchive = 0;
        int totalEntryArchive = 0;

        final File zipFile = Path.of(src).toFile();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                totalEntryArchive += 1;

                final Path destFile = Files.createFile(Path.of(dest, ze.getName()));
                final byte[] content = zis.readAllBytes();
                log.info("Unzipping: {} real size: {} compressed size: {}", ze.getName(), content.length, ze.getCompressedSize());
                final double compressionRatio = (double) content.length / ze.getCompressedSize();
                if (compressionRatio > THRESHOLD_RATIO) {
                    throw new IOException("Ratio between compressed and uncompressed data is highly suspicious, looks like a Zip Bomb Attack");
                }

                if (totalSizeArchive > THRESHOLD_SIZE) {
                    throw new IOException("The uncompressed data size is too much for the application resource capacity");
                }

                if (totalEntryArchive > THRESHOLD_ENTRIES) {
                    throw new IOException("Too much entries in this archive, can lead to inodes exhaustion of the system");
                }

                totalSizeArchive += content.length;
                Files.write(destFile, content);
                ze = zis.getNextEntry();
            }
        }
    }
}
