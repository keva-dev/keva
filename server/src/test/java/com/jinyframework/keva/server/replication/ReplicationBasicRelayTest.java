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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
class ReplicationBasicRelayTest {
    @SneakyThrows
    IServer startMaster(String host, int port) {
        final String snapLoc = Path.of("mastertest" + port).toString();
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
                log.warn(e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(Path.of(snapLoc, "KevaData"));
                    Files.deleteIfExists(Path.of(snapLoc, "KevaDataIndex"));
                    Files.deleteIfExists(Path.of(snapLoc, "data.zip"));
                    Files.deleteIfExists(Path.of(snapLoc));
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            }
        }).start();

        return server;
    }

    @SneakyThrows
    IServer startSlave(String host, int port, String master) {
        final String snapLoc = Path.of("slavetest" + port).toString();
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
                log.warn(e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(Path.of(snapLoc, "KevaData"));
                    Files.deleteIfExists(Path.of(snapLoc, "KevaDataIndex"));
                    Files.deleteIfExists(Path.of(snapLoc, "data.zip"));
                    Files.deleteIfExists(Path.of(snapLoc));
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            }
        }).start();

        return server;
    }

    @Test
    @Timeout(25)
    void masterForwardSlave() throws Exception {
        final int masterPort = PortUtil.getAvailablePort();
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(5);
        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(10);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        final SocketClient slave1Client = new SocketClient("localhost", slave1Port);
        final SocketClient slave2Client = new SocketClient("localhost", slave2Port);
        slave1Client.connect();
        slave2Client.connect();

        assertEquals("null", slave1Client.exchange("get abc"));
        assertEquals("null", slave2Client.exchange("get abc"));
        assertEquals("1", masterClient.exchange("set abc helloslave"));
        assertEquals("helloslave", slave1Client.exchange("get abc"));
        assertEquals("helloslave", slave2Client.exchange("get abc"));

        masterClient.disconnect();
        slave1Client.disconnect();
        slave2Client.disconnect();
        master.shutdown();
        slave1.shutdown();
        slave2.shutdown();
    }

    @Test
    @Timeout(25)
    void slaveHasPreviousMasterData() throws Exception {
        final int masterPort = PortUtil.getAvailablePort();
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(6);
        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();

        assertEquals("1", masterClient.exchange("set old oldmasterdata"));

        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(10);

        final SocketClient slave1Client = new SocketClient("localhost", slave1Port);
        slave1Client.connect();
        final SocketClient slave2Client = new SocketClient("localhost", slave1Port);
        slave2Client.connect();

        assertEquals("oldmasterdata", slave1Client.exchange("get old"));
        assertEquals("oldmasterdata", slave2Client.exchange("get old"));

        masterClient.disconnect();
        slave1Client.disconnect();
        master.shutdown();
        slave1.shutdown();
        slave2.shutdown();
    }

    @Test
    @Timeout(20)
    void slaveInfoUpdated() throws Exception {
        final int masterPort = PortUtil.getAvailablePort();
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(5);
        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(10);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        String info = masterClient.exchange("info");
        log.info(info);
        final String slaveInfoFormat = "(host:%s, port:%d, online:%b)";
        assertTrue(info.contains(String.format(slaveInfoFormat, "localhost", slave1Port, true)));
        assertTrue(info.contains(String.format(slaveInfoFormat, "localhost", slave2Port, true)));
        slave2.shutdown();
        TimeUnit.SECONDS.sleep(4);
        info = masterClient.exchange("info");
        assertTrue(info.contains(String.format(slaveInfoFormat, "localhost", slave2Port, false)));

        masterClient.disconnect();
        master.shutdown();
        slave1.shutdown();
    }
}
