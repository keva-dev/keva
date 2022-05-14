package com.keva.config;

import com.keva.config.util.ArgsHolder;
import com.keva.config.util.ArgsParser;
import com.keva.config.util.ConfigLoaderUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public final class ConfigLoader {
    public static final String DEFAULT_FILE_PATH = Paths.get(".", "keva.properties").toString();

    public static <T> T loadConfig(String[] args, Class<T> clazz) throws IOException {
        T returnConf = ConfigLoaderUtil.fromProperties(new Properties(), clazz);
        ArgsHolder config = ArgsParser.parse(args);
        T overrider = ConfigLoaderUtil.fromArgs(config, clazz);

        String configFilePath = config.getArgVal(new String[]{"f"});
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
        Properties props = new Properties();
        try (FileInputStream file = new FileInputStream(filePath)) {
            props.load(file);
        }

        return ConfigLoaderUtil.fromProperties(props, clazz);
    }
}
