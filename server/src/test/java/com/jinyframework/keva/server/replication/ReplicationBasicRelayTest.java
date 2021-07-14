package com.jinyframework.keva.server.replication;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
class ReplicationBasicRelayTest {
    @SneakyThrows
    IServer startMaster(String host, int port) {
        final String snapLoc = "./mastertest/";
        Files.createDirectories(Path.of(snapLoc));
        Files.deleteIfExists(Path.of(snapLoc, "KevaData"));
        Files.deleteIfExists(Path.of(snapLoc, "KevaDataIndex"));

        final IServer server = new NettyServer(ConfigHolder.defaultBuilder()
                                                           .snapshotEnabled(true)
                                                           .snapshotLocation(snapLoc)
                                                           .hostname(host)
                                                           .port(port)
                                                           .build());

        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        return server;
    }

    @SneakyThrows
    IServer startSlave(String host, int port, String master) {
        final String snapLoc = "./slavetest/";
        Files.createDirectories(Path.of(snapLoc));
        Files.deleteIfExists(Path.of(snapLoc, "KevaData"));
        Files.deleteIfExists(Path.of(snapLoc, "KevaDataIndex"));

        final IServer server = new NettyServer(ConfigHolder.defaultBuilder()
                                                           .snapshotEnabled(true)
                                                           .snapshotLocation(snapLoc)
                                                           .replicaOf(master)
                                                           .hostname(host)
                                                           .port(port)
                                                           .build());

        new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error(e.getMessage());
                System.exit(1);
            }
        }).start();

        return server;
    }

    @Test
    @Timeout(25)
    void masterForwardSlave() throws Exception {
        final int masterPort = PortUtil.getAvailablePort();
        final int slavePort = PortUtil.getAvailablePort();
        final IServer master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(5);
        final IServer slave = startSlave("localhost", slavePort, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(10);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        final SocketClient slaveClient = new SocketClient("localhost", slavePort);
        slaveClient.connect();

        String getAbc = slaveClient.exchange("get abc");
        assertEquals("null", getAbc);
        final String setAbc = masterClient.exchange("set abc helloslave");
        assertEquals("1", setAbc);
        getAbc = slaveClient.exchange("get abc");
        assertEquals("helloslave", getAbc);

        masterClient.disconnect();
        slaveClient.disconnect();
        master.shutdown();
        slave.shutdown();
    }

    @Test
    @Timeout(25)
    void slaveHasPreviousMasterData() throws Exception {
        final int masterPort = PortUtil.getAvailablePort();
        final int slavePort = PortUtil.getAvailablePort();
        final IServer master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(5);
        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();

        final String setOld = masterClient.exchange("set old oldmasterdata");
        assertEquals("1", setOld);

        final IServer slave = startSlave("localhost", slavePort, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(10);

        final SocketClient slaveClient = new SocketClient("localhost", slavePort);
        slaveClient.connect();

        String getAbc = slaveClient.exchange("get abc");
        assertEquals("null", getAbc);
        final String setAbc = masterClient.exchange("set abc helloslave");
        assertEquals("1", setAbc);
        getAbc = slaveClient.exchange("get abc");
        assertEquals("helloslave", getAbc);

        final String getOld = slaveClient.exchange("get old");
        assertEquals("oldmasterdata", getOld);

        masterClient.disconnect();
        slaveClient.disconnect();
        master.shutdown();
        slave.shutdown();
    }
}
