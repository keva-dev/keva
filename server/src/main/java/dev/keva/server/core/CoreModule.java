package dev.keva.server.core;

import dev.keva.server.command.setup.CommandService;
import dev.keva.store.StorageService;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CoreModule {
    private final StorageService storageService;
    private final CommandService commandService;
}
