package dev.keva.core.config;

import com.keva.config.annotation.CliProp;
import com.keva.config.annotation.CliPropType;
import com.keva.config.annotation.ConfigProp;
import dev.keva.ioc.annotation.Bean;
import dev.keva.ioc.annotation.Configuration;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode()
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Configuration
public class KevaConfig {
    @ConfigProp(name = "hostname", defaultVal = "localhost")
    @CliProp(name = {"h", "host"}, type = CliPropType.VAL)
    private String hostname;

    @ConfigProp(name = "port", defaultVal = "6379")
    @CliProp(name = {"p", "port"}, type = CliPropType.VAL)
    private Integer port;

    @ConfigProp(name = "save", defaultVal = "true")
    @CliProp(name = {"save", "s"}, type = CliPropType.FLAG)
    private Boolean persistence;

    @ConfigProp(name = "appendonly", defaultVal = "false")
    @CliProp(name = "appendonly", type = CliPropType.FLAG)
    private Boolean aof;

    @ConfigProp(name = "appendfsync", defaultVal = "1000")
    @CliProp(name = "appendfsync", type = CliPropType.VAL)
    private Integer aofInterval;

    @ConfigProp(name = "dir", defaultVal = "./")
    @CliProp(name = "dir", type = CliPropType.VAL)
    private String workDirectory;

    @ConfigProp(name = "requirepass", defaultVal = "")
    @CliProp(name = "requirepass", type = CliPropType.VAL)
    private String password;

    @ConfigProp(name = "io-threads", defaultVal = "-1")
    @CliProp(name = "io-threads", type = CliPropType.VAL)
    private Integer ioThreads;

    @Bean
    public static KevaConfig ofDefaults() {
        return builder()
                .workDirectory("./")
                .hostname("localhost")
                .port(6379)
                .persistence(true)
                .aof(false)
                .aofInterval(1000)
                .build();
    }
}
