package dev.keva.server.config;

import dev.keva.server.config.util.ArgsParser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public final class ConfigManager {
    public static final String DEFAULT_FILE_PATH = Paths.get(".", "keva.properties").toString();

    public static KevaConfig loadConfig(String[] args) throws IOException {
        var returnConf = ConfigLoader.fromProperties(new Properties(), KevaConfig.class);
        val config = ArgsParser.parse(args);
        val overrider = ConfigLoader.fromArgs(config, KevaConfig.class);

        val configFilePath = config.getArgVal("f");
        if (configFilePath != null) {
            returnConf = loadConfigFromFile(configFilePath);
        }

        ConfigLoader.merge(returnConf, overrider);
        log.info(returnConf.toString());
        return returnConf;
    }

    public static KevaConfig loadConfigFromFile(String filePath) throws IOException {
        if (filePath.isEmpty()) {
            filePath = DEFAULT_FILE_PATH;
        }
        val props = new Properties();
        try (val file = new FileInputStream(filePath)) {
            props.load(file);
        }

        return ConfigLoader.fromProperties(props, KevaConfig.class);
    }
}
