package dev.keva.core.config;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.storage.DatabaseFactory;
import dev.keva.storage.KevaDatabase;
import dev.keva.storage.impl.chroniclemap.ChronicleMapConfig;

@Configuration
public class DBConfig {
    @Autowired
    private KevaConfig kevaConfig;

    @Bean
    public KevaDatabase setupKevaDatabase() {
        ChronicleMapConfig dbConfig = ChronicleMapConfig.builder()
                .isPersistence(kevaConfig.getPersistence())
                .workingDirectory(kevaConfig.getWorkDirectory())
                .build();
        return DatabaseFactory.createChronicleMapDatabase(dbConfig);
    }
}
