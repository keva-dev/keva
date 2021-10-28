package dev.keva.server.command.base;

import dev.keva.server.core.AppFactory;
import dev.keva.store.StorageService;

public abstract class BaseCommandImpl {
    protected static final StorageService storageService = AppFactory.getStorageService();
}
