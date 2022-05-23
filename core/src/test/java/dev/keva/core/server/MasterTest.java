package dev.keva.core.server;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.replication.ReplicationCommand;
import dev.keva.core.utils.PortUtil;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.KevaDatabase;
import dev.keva.store.impl.OffHeapDatabaseImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.SafeEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class MasterTest {

    static String host = "localhost";
    static int port = PortUtil.getAvailablePort();
    private static KevaServer server;
    private static Jedis jedis;

    @BeforeAll
    static void startServer() throws Exception {
        val config = KevaConfig.builder()
            .persistence(true)
            .workDirectory("./")
            .aof(false)
            .hostname(host)
            .port(port)
            .build();
        server = KevaServer.of(config);

        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        // Wait for server to start
        TimeUnit.SECONDS.sleep(5);

        jedis = new Jedis(host, port);
    }

    @AfterAll
    static void stop() {
        jedis.disconnect();
        server.shutdown();
    }

    @Test
    public void testPsync() throws IOException {
        jedis.set("abc", "edf");
        byte[] rawResponse = (byte[]) jedis.sendBlockingCommand(ReplicationCommand.PSYNC, "", "0");
        String response = SafeEncoder.encode(rawResponse);
        assertTrue(response.startsWith("FULLRESYNC"));
        String[] fullResyncRes = response.split(" ");
        String masterRepId = fullResyncRes[1];
        assertNotNull(masterRepId);
        String masterOffsetStr = fullResyncRes[2];
        long masterOffset = Long.parseLong(masterOffsetStr);
        assertTrue(masterOffset >= 0);
        byte[] fileContent = jedis.getClient().getBinaryBulkReply();
        Path snapshot = Paths.get("./mastertest/dump.kdb");
        deleteDir(new File("mastertest"));
        Files.createDirectory(Paths.get("mastertest"));
        Files.write(snapshot, fileContent);
        KevaDatabase database = new OffHeapDatabaseImpl(DatabaseConfig.builder()
            .isPersistence(true)
            .workingDirectory("./mastertest")
            .build());
        byte[] valueInBytes = database.get("abc".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("edf".getBytes(StandardCharsets.UTF_8), valueInBytes);
        List<String> multiBulkReply = jedis.getClient().getMultiBulkReply();
        assertTrue("set abc edf".equalsIgnoreCase(multiBulkReply.get(0)));
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

}
