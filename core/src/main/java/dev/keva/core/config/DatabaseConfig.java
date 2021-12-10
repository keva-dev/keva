package dev.keva.core.config;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.store.DatabaseFactory;
import dev.keva.store.KevaDatabase;
import lombok.val;

@Configuration
public class DatabaseConfig {
    @Autowired
    private KevaConfig kevaConfig;

    @Bean
    public KevaDatabase setupKevaDatabase() {
        val dbConfig = dev.keva.store.DatabaseConfig.builder()
                .isPersistence(kevaConfig.getPersistence())
                .workingDirectory(kevaConfig.getWorkDirectory())
                .build();
        return DatabaseFactory.createOffHeapDatabase(dbConfig);
    }
}
