package com.jinyframework.keva.server.config;

import com.jinyframework.keva.server.util.ArgsParser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public final class ConfigManager {
    public static final String DEFAULT_FILE_PATH = Paths.get(".", "keva.properties").toString();
    private static ConfigHolder configHolder;

    private ConfigManager() {
    }

    public static ConfigHolder getConfig() {
        return configHolder;
    }

    public static void setConfig(ConfigHolder config) {
        configHolder = config;
    }

    public static void loadConfig(String[] args) throws Exception {
        val config = ArgsParser.parse(args);
        log.info(config.toString());
        val overrider = ConfigHolder.fromArgs(config);

        val configFilePath = config.getArgVal("f");
        if (configFilePath != null) {
            loadConfigFromFile(configFilePath);
        } else {
            // init using with default values
            setConfig(ConfigHolder.fromProperties(new Properties()));
        }

        configHolder.merge(overrider);
    }

    public static void loadConfigFromFile(String filePath) throws Exception {
        if (filePath.isEmpty()) {
            filePath = DEFAULT_FILE_PATH;
        }
        val props = new Properties();
        try (val file = new FileInputStream(filePath)) {
            props.load(file);
        }

        configHolder = ConfigHolder.fromProperties(props);
    }
}
