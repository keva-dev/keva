package dev.keva.server.config;

import dev.keva.server.config.annotation.CliProp;
import dev.keva.server.config.annotation.CliPropType;
import dev.keva.server.config.annotation.ConfigProp;
import dev.keva.server.config.util.ArgsHolder;
import lombok.*;

import java.util.Properties;

@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode
public class ConfigHolder {
    @ConfigProp(name = "hostname", defaultVal = "localhost")
    @CliProp(name = "h", type = CliPropType.VAL)
    private String hostname;

    @ConfigProp(name = "port", defaultVal = "6767")
    @CliProp(name = "p", type = CliPropType.VAL)
    private Integer port;

    @ConfigProp(name = "snapshot_enabled", defaultVal = "true")
    @CliProp(name = "ss", type = CliPropType.FLAG)
    private Boolean snapshotEnabled;

    @ConfigProp(name = "snapshot_location", defaultVal = "./")
    @CliProp(name = "sl", type = CliPropType.VAL)
    private String snapshotLocation;

    @ConfigProp(name = "heap_size", defaultVal = "64")
    @CliProp(name = "hs", type = CliPropType.VAL)
    private Integer heapSize;

    @SneakyThrows
    public static ConfigHolder fromProperties(@NonNull Properties props) {
        val configHolder = builder().build();
        val fields = ConfigHolder.class.getDeclaredFields();
        for (val field : fields) {
            if (field.isAnnotationPresent(ConfigProp.class)) {
                val annotation = field.getAnnotation(ConfigProp.class);
                val value = parse(props.getProperty(annotation.name(), annotation.defaultVal()), field.getType());
                field.set(configHolder, value);
            }
        }

        return configHolder;
    }

    @SneakyThrows
    public static ConfigHolder fromArgs(@NonNull ArgsHolder args) {
        val configHolder = builder().build();

        val fields = ConfigHolder.class.getDeclaredFields();
        for (val field : fields) {
            if (field.isAnnotationPresent(CliProp.class)) {
                val cliAnnotate = field.getAnnotation(CliProp.class);
                String strVal = null;
                if (cliAnnotate.type() == CliPropType.VAL) {
                    strVal = args.getArgVal(cliAnnotate.name());
                } else if (cliAnnotate.type() == CliPropType.FLAG) {
                    strVal = args.getFlag(cliAnnotate.name());
                }
                if (strVal != null) {
                    val value = parse(strVal, field.getType());
                    field.set(configHolder, value);
                }
            }
        }

        return configHolder;
    }

    @SneakyThrows
    private static <T> T parse(String s, Class<T> clazz) {
        return clazz.getConstructor(String.class).newInstance(s);
    }

    public static ConfigHolderBuilder defaultBuilder() {
        return builder()
                .snapshotLocation("./")
                .hostname("localhost")
                .port(6767)
                .heapSize(64)
                .snapshotEnabled(true);
    }

    public static ConfigHolder makeDefaultConfig() {
        return builder()
                .snapshotLocation("./")
                .hostname("localhost")
                .port(6767)
                .heapSize(64)
                .snapshotEnabled(true)
                .build();
    }

    @SneakyThrows
    public ConfigHolder merge(ConfigHolder overrideHolder) {
        if (overrideHolder != null && !equals(overrideHolder)) {
            for (val field : overrideHolder.getClass().getDeclaredFields()) {
                val overrideVal = field.get(overrideHolder);
                if (overrideVal != null) {
                    field.set(this, overrideVal);
                }
            }
            return this;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Configurations: " +
                "hostname=" + hostname +
                ", port=" + port +
                ", snapshotEnabled=" + snapshotEnabled +
                ", snapshotLocation='" + snapshotLocation + '\'' +
                ", heapSize=" + heapSize;
    }
}
