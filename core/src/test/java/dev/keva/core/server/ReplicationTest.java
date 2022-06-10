package dev.keva.core.server;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.utils.PortUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ReplicationTest {

    static String host = "localhost";

    @SneakyThrows
    @Test
    public void bufferRelayTest() {
        int masterPort = PortUtil.getAvailablePort();
        String masterFolder = "./master_test";
        TestUtil.deleteDir(new File(masterFolder));
        Files.createDirectory(Paths.get(masterFolder));
        KevaConfig masterConfig = KevaConfig.builder()
            .workDirectory(masterFolder)
            .hostname(host)
            .aof(false)
            .persistence(true)
            .port(masterPort)
            .build();
        KevaServer masterServer = KevaServer.of(masterConfig);

        new Thread(masterServer).start();
        TimeUnit.SECONDS.sleep(5);

        Jedis masterJedis = new Jedis(host, masterPort);
        masterJedis.set("abcd", "before");

        int slavePort = PortUtil.getAvailablePort();
        String slaveFolder = "./slave_test";
        TestUtil.deleteDir(new File(slaveFolder));
        Files.createDirectory(Paths.get(slaveFolder));
        KevaConfig slaveConfig = KevaConfig.builder()
            .workDirectory(slaveFolder)
            .hostname(host)
            .persistence(true)
            .aof(false)
            .port(slavePort)
            .replicaOf(host + " " + masterPort)
            .build();
        KevaServer slaveServer = KevaServer.of(slaveConfig);
        new Thread(slaveServer).start();
        TimeUnit.SECONDS.sleep(5);

        Jedis slaveJedis = new Jedis(host, slavePort, Integer.MAX_VALUE);
        assertEquals("before", slaveJedis.get("abcd"));

        masterJedis.set("after", "wards");
        assertEquals("wards", slaveJedis.get("after"));
        masterJedis.set("after", "second");
        assertEquals("second", slaveJedis.get("after"));

        masterServer.shutdown();
        slaveServer.shutdown();
    }

}
