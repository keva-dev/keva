package com.jinyframework.keva.proxy.balance;

public interface LoadBalancingService {
	void addShard(String endpoint, int virtualNodeAmount);

	void forwardRequest(Request request, String identifier);
}
