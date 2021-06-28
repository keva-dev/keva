package com.jinyframework.keva.server.replication;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Builder
@Data
public class ReplicaInfo {
    AtomicLong lastCommunicated;
}
