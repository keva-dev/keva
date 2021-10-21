package com.jinyframework.keva.server.replication;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.core.IServer;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.util.PortUtil;
import com.jinyframework.keva.server.util.SocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    IServer master;
    int masterPort;

    @SneakyThrows
    IServer startMaster(String host, int port) {
        final String snapLoc = Path.of("mastertest" + port).toString();
        Files.createDirectories(Path.of(snapLoc));

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
                    Files.deleteIfExists(Path.of(snapLoc, "dump.kdb"));
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

        final IServer server = new NettyServer(ConfigHolder.defaultBuilder()
                .snapshotEnabled(true)
                .snapshotLocation(snapLoc)
                .replicaOf(master)
                .hostname(host)
                .port(port)
                .build());

        new Thread(server).start();

        return server;
    }

    @SneakyThrows
    void cleanUpSlave(String snapLoc) {
        Files.deleteIfExists(Path.of(snapLoc, "dump.kdb"));
        Files.deleteIfExists(Path.of(snapLoc));
    }

    @BeforeEach
    void initMaster() throws Exception {
        masterPort = PortUtil.getAvailablePort();
        master = startMaster("localhost", masterPort);
        TimeUnit.SECONDS.sleep(6);
    }

    @AfterEach
    void cleanupMaster() {
        if (master != null) {
            master.shutdown();
        }
    }

    @Test
    @Timeout(25)
    void masterForwardSlave() throws Exception {
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(6);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        final SocketClient slave1Client = new SocketClient("localhost", slave1Port);
        final SocketClient slave2Client = new SocketClient("localhost", slave2Port);
        slave1Client.connect();
        slave2Client.connect();

        assertEquals("null", slave1Client.exchange("get abc"));
        assertEquals("null", slave2Client.exchange("get abc"));
        assertEquals("1", masterClient.exchange("set abc helloslave"));
        TimeUnit.MILLISECONDS.sleep(1);
        assertEquals("helloslave", slave1Client.exchange("get abc"));
        assertEquals("helloslave", slave2Client.exchange("get abc"));

        masterClient.disconnect();
        slave1Client.disconnect();
        slave2Client.disconnect();
        slave1.shutdown();
        slave2.shutdown();
        cleanUpSlave("slavetest" + slave1Port);
        cleanUpSlave("slavetest" + slave2Port);
    }

    @Test
    @Timeout(25)
    void slaveHasPreviousMasterData() throws Exception {
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();

        assertEquals("1", masterClient.exchange("set old oldmasterdata"));

        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(6);

        final SocketClient slave1Client = new SocketClient("localhost", slave1Port);
        slave1Client.connect();
        final SocketClient slave2Client = new SocketClient("localhost", slave1Port);
        slave2Client.connect();

        assertEquals("oldmasterdata", slave1Client.exchange("get old"));
        assertEquals("oldmasterdata", slave2Client.exchange("get old"));

        masterClient.disconnect();
        slave1Client.disconnect();
        slave2Client.disconnect();
        slave1.shutdown();
        slave2.shutdown();
        cleanUpSlave("slavetest" + slave1Port);
        cleanUpSlave("slavetest" + slave2Port);
    }

    @Test
    @Timeout(20)
    void slaveInfoUpdated() throws Exception {
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(1); // ensure slave 1 is registered first
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(6);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        String info = masterClient.exchange("info");
        log.info(info);
        final String slaveInfoFormat = "(host:%s, port:%d, online:%b)";
        assertTrue(info.contains(String.format("slave0=" + slaveInfoFormat, "localhost", slave1Port, true)));
        assertTrue(info.contains(String.format("slave1=" + slaveInfoFormat, "localhost", slave2Port, true)));
        slave2.shutdown();
        TimeUnit.SECONDS.sleep(4);
        info = masterClient.exchange("info");
        assertTrue(info.contains(String.format("slave0=" + slaveInfoFormat, "localhost", slave1Port, true)));
        assertTrue(info.contains(String.format("slave1=" + slaveInfoFormat, "localhost", slave2Port, false)));

        masterClient.disconnect();
        slave1.shutdown();
        cleanUpSlave("slavetest" + slave1Port);
        cleanUpSlave("slavetest" + slave2Port);
    }

    @Test
    @Timeout(25)
    void partialSync() throws Exception {
        final int slave1Port = PortUtil.getAvailablePort();
        final int slave2Port = PortUtil.getAvailablePort();
        final IServer slave1 = startSlave("localhost", slave1Port, "localhost:" + masterPort);
        final IServer slave2 = startSlave("localhost", slave2Port, "localhost:" + masterPort);
        TimeUnit.SECONDS.sleep(6);

        final SocketClient masterClient = new SocketClient("localhost", masterPort);
        masterClient.connect();
        final SocketClient slave1Client = new SocketClient("localhost", slave1Port);
        final SocketClient slave2Client = new SocketClient("localhost", slave2Port);
        slave1Client.connect();
        slave2Client.connect();

        assertEquals("null", slave1Client.exchange("get abc"));
        assertEquals("null", slave2Client.exchange("get abc"));
        assertEquals("1", masterClient.exchange("set abc helloslave"));
        TimeUnit.MILLISECONDS.sleep(1);
        assertEquals("helloslave", slave1Client.exchange("get abc"));
        assertEquals("helloslave", slave2Client.exchange("get abc"));


        slave2.shutdown();
        slave2Client.disconnect();
        // wait for it to shut down
        TimeUnit.SECONDS.sleep(2);

        assertEquals("1", masterClient.exchange("set psync success"));

        new Thread(() -> slave2.run(false)).start();
        // wait for slave2 to restart and perform partial sync
        TimeUnit.SECONDS.sleep(6);

        slave2Client.connect();
        assertEquals("helloslave", slave1Client.exchange("get abc"));
        assertEquals("helloslave", slave2Client.exchange("get abc"));
        assertEquals("success", slave2Client.exchange("get psync"));
        assertEquals("success", slave1Client.exchange("get psync"));

        masterClient.disconnect();
        slave1Client.disconnect();
        slave2Client.disconnect();
        slave1.shutdown();
        slave2.shutdown();
        cleanUpSlave("slavetest" + slave1Port);
        cleanUpSlave("slavetest" + slave2Port);
    }
}
