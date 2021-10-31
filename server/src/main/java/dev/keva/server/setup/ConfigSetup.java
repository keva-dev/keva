package dev.keva.server.setup;

import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import dev.keva.server.config.KevaConfig;

@Configuration
public class ConfigSetup {
    @Bean
    public KevaConfig setupKevaConfig() {
        return KevaConfig.ofDefaults();
    }
}
