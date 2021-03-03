package com.jinyframework.keva.server.config;

import com.jinyframework.keva.server.util.ArgsHolder;
import lombok.*;

import java.util.Properties;

@Builder(toBuilder = true)
@Getter
@Setter
public class ConfigHolder {

    @ConfigProp(name = "heartbeat_enabled", defaultVal = "false")
    @CliProp(name = "hb", type = CliPropType.FLAG)
    private Boolean heartbeatEnabled;

    @ConfigProp(name = "snapshot_enabled", defaultVal = "false")
    @CliProp(name = "ss", type = CliPropType.FLAG)
    private Boolean snapshotEnabled;

    @ConfigProp(name = "hostname", defaultVal = "localhost")
    @CliProp(name = "h", type = CliPropType.VAL)
    private String hostname;

    @ConfigProp(name = "port", defaultVal = "6767")
    @CliProp(name = "p", type = CliPropType.VAL)
    private Integer port;

    @ConfigProp(name = "heartbeat_timeout", defaultVal = "120000")
    @CliProp(name = "ht", type = CliPropType.VAL)
    private Long heartbeatTimeout;

    @ConfigProp(name = "snapshot_interval", defaultVal = "PT2M")
    @CliProp(name = "sn", type = CliPropType.VAL)
    private String snapshotInterval;

    @ConfigProp(name = "backup_path", defaultVal = "./dump.keva")
    @CliProp(name = "bk", type = CliPropType.VAL)
    private String backupPath;

    @ConfigProp(name = "recovery_path", defaultVal = "./dump.keva")
    @CliProp(name = "rc", type = CliPropType.VAL)
    private String recoveryPath;

    public static ConfigHolder fromProperties(@NonNull Properties props) throws Exception {
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

    public static ConfigHolder fromArgs(@NonNull ArgsHolder args) throws Exception {
        val configHolder = builder().build();

        val fields = ConfigHolder.class.getDeclaredFields();
        for (val field : fields) {
            if (field.isAnnotationPresent(CliProp.class)) {
                val cliAnnot = field.getAnnotation(CliProp.class);
                String strVal = null;
                switch (cliAnnot.type()) {
                    case VAL:
                        strVal = args.getArgVal(cliAnnot.name());
                        break;
                    case FLAG:
                        strVal = args.getFlag(cliAnnot.name());
                        break;
                }
                if (strVal != null) {
                    val value = parse(strVal, field.getType());
                    field.set(configHolder, value);
                }
            }
        }

        return configHolder;
    }

    public void merge(ConfigHolder overrideHolder) throws Exception {
        if (overrideHolder != null) {
            for (val field : overrideHolder.getClass().getDeclaredFields()) {
                val overrideVal = field.get(overrideHolder);
                if (overrideVal != null) {
                    field.set(this,overrideVal);
                }
            }
        }
    }

    private static <T> T parse(String s, Class<T> clazz) throws Exception {
        return clazz.getConstructor(new Class[]{String.class}).newInstance(s);
    }

    @Override
    public String toString() {
        return "ConfigHolder{" +
                "heartbeatEnabled=" + heartbeatEnabled +
                ", snapshotEnabled=" + snapshotEnabled +
                ", hostname='" + hostname + '\'' +
                ", port=" + port +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", snapshotInterval='" + snapshotInterval + '\'' +
                ", backupPath='" + backupPath + '\'' +
                ", recoveryPath='" + recoveryPath + '\'' +
                '}';
    }
}
