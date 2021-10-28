package dev.keva.server.command.base;

import dev.keva.server.core.AppFactory;
import dev.keva.store.KevaDatabase;

public abstract class BaseCommandImpl {
    protected static final KevaDatabase database = AppFactory.getKevaDatabase();
}
