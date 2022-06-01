package dev.keva.core.config;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.storage.DatabaseConfig;
import dev.keva.storage.DatabaseFactory;
import dev.keva.storage.KevaDatabase;

@Configuration
public class DBConfig {
    @Autowired
    private KevaConfig kevaConfig;

    @Bean
    public KevaDatabase setupKevaDatabase() {
        DatabaseConfig dbConfig = DatabaseConfig.builder()
                .isPersistence(kevaConfig.getPersistence())
                .workingDirectory(kevaConfig.getWorkDirectory())
                .build();
        return DatabaseFactory.createChronicleMapDatabase(dbConfig);
    }
}
