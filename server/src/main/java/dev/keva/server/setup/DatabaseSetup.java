package dev.keva.server.setup;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.server.config.KevaConfig;
import dev.keva.store.DatabaseConfig;
import dev.keva.store.DatabaseFactory;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Configuration
public class DatabaseSetup {
    @Autowired
    private KevaConfig kevaConfig;

    @Bean
    public KevaDatabase setupKevaDatabase() {
        val dbConfig = DatabaseConfig.builder()
                .heapSize(kevaConfig.getHeapSize())
                .snapshotEnabled(kevaConfig.getPersistence())
                .snapshotLocation(kevaConfig.getWorkDirectory())
                .build();
        return DatabaseFactory.createChronicleMap(dbConfig);
    }
}
