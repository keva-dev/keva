package dev.keva.server.command.impl.transaction.manager;

import dev.keva.ioc.annotation.Component;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TransactionManager {
    @Getter
    private final ConcurrentMap<Channel, TransactionContext> transactions = new ConcurrentHashMap<>();

    @Getter
    private final ReentrantLock transactionLock = new ReentrantLock();
}
