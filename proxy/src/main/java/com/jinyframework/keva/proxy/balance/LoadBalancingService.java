package com.jinyframework.keva.proxy.balance;

public interface LoadBalancingService {
	void addShard(String endpoint, int virtualNodeAmount);

	String forwardRequest(String request, String identifier);
}
