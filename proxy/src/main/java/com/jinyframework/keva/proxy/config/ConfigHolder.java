package com.jinyframework.keva.proxy.config;

import java.util.Properties;

import config.config.CliProp;
import config.config.CliPropType;
import config.config.ConfigProp;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import util.ArgsHolder;

@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ConfigHolder {

    @ConfigProp(name = "hostname", defaultVal = "localhost")
    @CliProp(name = "h", type = CliPropType.VAL)
    private String hostname;

    @ConfigProp(name = "port", defaultVal = "6767")
    @CliProp(name = "p", type = CliPropType.VAL)
    private Integer port;

    @ConfigProp(name = "server_list", defaultVal = "")
    @CliProp(name = "sl", type = CliPropType.VAL)
    private String serverList;

    @ConfigProp(name = "virtual_node_amount", defaultVal = "3")
    @CliProp(name = "vna", type = CliPropType.VAL)
    private Integer virtualNodeAmount;

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
                .hostname("localhost")
                .port(6767);
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
}
