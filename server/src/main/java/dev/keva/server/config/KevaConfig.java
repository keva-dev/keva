package dev.keva.server.config;

import dev.keva.server.config.annotation.CliProp;
import dev.keva.server.config.annotation.CliPropType;
import dev.keva.server.config.annotation.ConfigProp;
import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode()
@ToString
@NoArgsConstructor
@AllArgsConstructor
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

    @ConfigProp(name = "work_directory", defaultVal = "./")
    @CliProp(name = "dir", type = CliPropType.VAL)
    private String workDirectory;

    /**
     * Helper method to build custom config based of the defaults
     * @return Builder with some sensible defaults already set
     */
    public static KevaConfigBuilder custom() {
        return builder()
                .workDirectory("./")
                .hostname("localhost")
                .port(6379)
                .persistence(true);
    }

    /**
     * @return KevaConfig with sensible defaults
     */
    public static KevaConfig ofDefaults() {
        return builder()
                .workDirectory("./")
                .hostname("localhost")
                .port(6379)
                .persistence(true)
                .build();
    }
}
