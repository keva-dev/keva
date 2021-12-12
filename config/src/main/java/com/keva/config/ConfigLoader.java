package com.keva.config;

import com.keva.config.util.ArgsParser;
import com.keva.config.util.ConfigLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public final class ConfigLoader {
    public static final String DEFAULT_FILE_PATH = Paths.get(".", "keva.properties").toString();

    public static <T> T loadConfig(String[] args, Class<T> clazz) throws IOException {
        T returnConf = ConfigLoaderUtil.fromProperties(new Properties(), clazz);
        val config = ArgsParser.parse(args);
        val overrider = ConfigLoaderUtil.fromArgs(config, clazz);

        val configFilePath = config.getArgVal("f");
        if (configFilePath != null) {
            returnConf = loadConfigFromFile(configFilePath, clazz);
        }

        ConfigLoaderUtil.merge(returnConf, overrider);
        log.info(returnConf.toString());
        return returnConf;
    }

    public static <T> T loadConfigFromFile(String filePath, Class<T> clazz) throws IOException {
        if (filePath.isEmpty()) {
            filePath = DEFAULT_FILE_PATH;
        }
        val props = new Properties();
        try (val file = new FileInputStream(filePath)) {
            props.load(file);
        }

        return ConfigLoaderUtil.fromProperties(props, clazz);
    }
}
