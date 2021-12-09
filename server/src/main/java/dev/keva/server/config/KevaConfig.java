package dev.keva.server.config;

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
    @CliProp(name = "h", type = CliPropType.VAL)
    private String hostname;

    @ConfigProp(name = "port", defaultVal = "6379")
    @CliProp(name = "p", type = CliPropType.VAL)
    private Integer port;

    @ConfigProp(name = "persistence", defaultVal = "true")
    @CliProp(name = "ps", type = CliPropType.FLAG)
    private Boolean persistence;

    @ConfigProp(name = "aof", defaultVal = "false")
    @CliProp(name = "aof", type = CliPropType.FLAG)
    private Boolean aof;

    @ConfigProp(name = "aof_interval", defaultVal = "1000")
    @CliProp(name = "ai", type = CliPropType.VAL)
    private Integer aofInterval;

    @ConfigProp(name = "work_directory", defaultVal = "./")
    @CliProp(name = "dir", type = CliPropType.VAL)
    private String workDirectory;

    @ConfigProp(name = "requirepass", defaultVal = "")
    @CliProp(name = "pw", type = CliPropType.VAL)
    private String password;

    /**
     * @return KevaConfig with sensible defaults
     */
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

    /**
     * Helper method to build custom config based of the defaults
     *
     * @return Builder with some sensible defaults already set
     */
    public static KevaConfigBuilder custom() {
        return builder()
                .workDirectory("./")
                .hostname("localhost")
                .port(6379)
                .persistence(true)
                .aof(false)
                .aofInterval(1000);
    }
}
