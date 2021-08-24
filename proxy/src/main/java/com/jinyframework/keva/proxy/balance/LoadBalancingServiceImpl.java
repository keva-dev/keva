package com.jinyframework.keva.proxy.balance;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;

import com.jinyframework.keva.proxy.util.HashUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadBalancingServiceImpl implements LoadBalancingService {
	private static final String SHARD_KEY = "shard_key";
	//Map of shard key and its corresponding shard connection
	private final ConcurrentHashMap<Long , Shard> shards = new ConcurrentHashMap<>();
	//Map of virtual node to use in consistent hashing
	private final ConcurrentSkipListMap<Long, Long> virtualNodes = new ConcurrentSkipListMap<>();

	private static InetSocketAddress parseEndpoint(String addr) {
		final String[] s = addr.split(":");
		final String host = s[0];
		final int port = Integer.parseInt(s[1]);
		return new InetSocketAddress(host, port);
	}

	private Shard getShard(String identifier) {
		ConcurrentNavigableMap<Long, Long> subMap = virtualNodes.tailMap(HashUtil.hash(identifier));
		Long key;
		if(subMap.isEmpty()){
			//If there is no one larger than the hash value of the key, start with the first node
			key = virtualNodes.firstKey();
		} else{
			//The first Key is the nearest node clockwise past the node.
			key = subMap.firstKey();
		}
		return shards.get(virtualNodes.get(key));
	}

	private static void forwardShardKey(Shard shard, long key) {
		shard.send("set " + SHARD_KEY + " " + key);
	}

	@Override
	public void addShard(String endpoint, int virtualNodeAmount) {
		final InetSocketAddress addr = parseEndpoint(endpoint);
		final Shard shard = new Shard(addr.getHostName(), addr.getPort());
		long shardKey = HashUtil.hash(endpoint);
		forwardShardKey(shard, shardKey);
		shards.put(shardKey, shard);
		for (int i = 0; i < virtualNodeAmount; i++) {
			virtualNodes.put(HashUtil.hash(endpoint + "VN" + i), shardKey);
		}
	}

	@Override
	public String forwardRequest(String request, String identifier) {
		Shard shard = this.getShard(identifier);
		try {
			return (String) shard.send(request).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error(e.getMessage());
			Thread.currentThread().interrupt();
			return "Something wrong happen";
		}
	}
}
