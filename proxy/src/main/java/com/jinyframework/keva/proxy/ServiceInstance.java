package com.jinyframework.keva.proxy;

import com.jinyframework.keva.proxy.balance.LoadBalancingService;
import com.jinyframework.keva.proxy.balance.LoadBalancingServiceImpl;
import com.jinyframework.keva.proxy.command.CommandService;
import com.jinyframework.keva.proxy.command.CommandServiceImpl;
import com.jinyframework.keva.server.core.ConnectionService;
import com.jinyframework.keva.server.core.ConnectionServiceImpl;
import com.jinyframework.keva.server.storage.NoHeapStorageServiceImpl;
import com.jinyframework.keva.server.storage.StorageService;
import lombok.Setter;

@Setter
public final class ServiceInstance {
    private ServiceInstance() {
    }

    public static ConnectionService getConnectionService() {
        return ConnectionServiceHolder.INSTANCE;
    }

    public static CommandService getCommandService() {
        return CommandServiceHolder.INSTANCE;
    }

    public static StorageService getStorageService() {
        return StorageServiceHolder.INSTANCE;
    }

    public static LoadBalancingService getLoadBalancingService() {
        return LoadBalancingServiceHolder.INSTANCE;
    }

    private static final class ConnectionServiceHolder {
        private static final ConnectionService INSTANCE = new ConnectionServiceImpl();
    }

    private static final class CommandServiceHolder {
        private static final CommandService INSTANCE = new CommandServiceImpl();
    }

    private static final class StorageServiceHolder {
        private static final StorageService INSTANCE = new NoHeapStorageServiceImpl();
    }

    private static final class LoadBalancingServiceHolder {
        private static final LoadBalancingService INSTANCE = new LoadBalancingServiceImpl();
    }
}
