package com.jinyframework.keva.server.replication.master;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Data
public class Replica {
    AtomicLong lastCommunicated;
    BlockingQueue<String> cmdBuffer;
    ReplicaClient client;
}
