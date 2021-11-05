package dev.keva.server.command.impl.transaction.manager;

import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;

import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class TransactionLock {
    @Bean("transactionLock")
    public ReentrantLock transactionLock() {
        return new ReentrantLock();
    }
}
