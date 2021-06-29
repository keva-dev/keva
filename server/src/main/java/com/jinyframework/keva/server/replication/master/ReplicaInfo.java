package com.jinyframework.keva.server.replication.master;

import lombok.Builder;
import lombok.Data;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Data
public class ReplicaInfo {
    AtomicLong lastCommunicated;
    Queue<String> cmdBuffer;
}
