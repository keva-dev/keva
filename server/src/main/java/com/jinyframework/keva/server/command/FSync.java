package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.storage.StorageService;

import java.io.File;
import java.util.List;

public class FSync implements CommandHandler{
    private final StorageService storageService = ServiceInstance.getStorageService();

    @Override
    public Object handle(List<String> args) {
        // register replica
        final String replicaHost = args.get(0);
        final String replicaPort = args.get(1);
        // send snapshot to replica
        final File file = storageService.getSnapshotPath().toFile();
        return "";
    }
}
