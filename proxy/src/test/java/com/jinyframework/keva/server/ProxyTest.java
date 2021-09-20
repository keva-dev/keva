package com.jinyframework.keva.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import com.jinyframework.keva.proxy.config.ConfigHolder;
import com.jinyframework.keva.proxy.core.NettyServer;
import com.jinyframework.keva.server.core.IServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import util.PortUtil;
import util.SocketClient;

@Slf4j
@DisplayName("Netty Server")
public class ProxyTest extends AbstractProxyTest {
	static String host = "localhost";
	static List<IServer> shards = new ArrayList<>();

	@BeforeAll
	static void startServer() throws Exception {
		int shard1Port = addShard();
		int shard2Port = addShard();

		int serverPort = PortUtil.getAvailablePort();
		server = new NettyServer(ConfigHolder.defaultBuilder()
			.hostname(host)
			.port(serverPort)
			.virtualNodeAmount(3)
			.serverList(host + ":" + shard1Port + "," + host + ":" + shard2Port)
			.build());

		new Thread(() -> {
			try {
				server.run();
			} catch (Exception e) {
				log.error(e.getMessage());
				System.exit(1);
			}
		}).start();

		// Wait for server to start
		TimeUnit.SECONDS.sleep(20);

		client = new SocketClient(host, serverPort);
		client.connect();
	}

	@AfterAll
	static void stop() {
		client.disconnect();
		server.shutdown();
		shards.forEach(IServer::shutdown);
	}

	// Return shard port (host is always local)
	private static int addShard() throws InterruptedException {
		val port= PortUtil.getAvailablePort();
		IServer shard = new com.jinyframework.keva.server.core.NettyServer(com.jinyframework.keva.server.config.ConfigHolder.defaultBuilder()
			.snapshotEnabled(false)
			.hostname(host)
			.port(port)
			.build());

		new Thread(() -> {
			try {
				shard.run();
			} catch (Exception e) {
				log.error(e.getMessage());
				System.exit(1);
			}
		}).start();

		TimeUnit.SECONDS.sleep(10);
		shards.add(shard);
		return port;
	}
}
