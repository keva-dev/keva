package dev.keva.core.command.impl.transaction.manager;

import dev.keva.ioc.annotation.Component;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TransactionManager {
    @Getter
    private final ConcurrentMap<Channel, TransactionContext> transactions = new ConcurrentHashMap<>();
}
