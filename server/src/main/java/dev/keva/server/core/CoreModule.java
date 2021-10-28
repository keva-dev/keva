package dev.keva.server.core;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import dev.keva.server.command.*;
import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.config.ConfigHolder;
import dev.keva.store.NoHeapConfig;
import dev.keva.store.NoHeapFactory;
import dev.keva.store.StorageService;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Builder
@Getter
@Slf4j
public class CoreModule extends AbstractModule {
    private final ConfigHolder config;

    public CoreModule(@NonNull ConfigHolder config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(StorageService.class).toInstance(provideStorageService());
        bind(CommandHandler.class).annotatedWith(Names.named("GET")).to(Get.class);
        bind(CommandHandler.class).annotatedWith(Names.named("SET")).to(Set.class);
        bind(CommandHandler.class).annotatedWith(Names.named("DEL")).to(Del.class);
        bind(CommandHandler.class).annotatedWith(Names.named("EXPIRE")).to(Expire.class);
        bind(CommandHandler.class).annotatedWith(Names.named("PING")).to(Ping.class);
        bind(CommandHandler.class).annotatedWith(Names.named("INFO")).to(Info.class);
    }

    private StorageService provideStorageService() {
        val noHeapConfig = NoHeapConfig.builder()
                .heapSize(config.getHeapSize())
                .snapshotEnabled(config.getSnapshotEnabled())
                .snapshotLocation(config.getSnapshotLocation())
                .build();
        return NoHeapFactory.makeNoHeapDBStore(noHeapConfig);
    }
}
