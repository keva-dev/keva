package com.jinyframework.keva.proxy.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import util.ArgsParser;

@Slf4j
public class ConfigManager {
	public static final String DEFAULT_FILE_PATH = Paths.get(".", "proxy.properties").toString();

	private ConfigManager() {
	}

	public static ConfigHolder loadConfig(String[] args) throws IOException {
		ConfigHolder returnConf = ConfigHolder.fromProperties(new Properties());
		val config = ArgsParser.parse(args);
		log.info(config.toString());
		val overrider = ConfigHolder.fromArgs(config);

		val configFilePath = config.getArgVal("f");
		if (configFilePath != null) {
			returnConf = loadConfigFromFile(configFilePath);
		}

		return returnConf.merge(overrider);
	}

	public static ConfigHolder loadConfigFromFile(String filePath) throws IOException {
		if (filePath.isEmpty()) {
			filePath = DEFAULT_FILE_PATH;
		}
		val props = new Properties();
		try (val file = new FileInputStream(filePath)) {
			props.load(file);
		}

		return ConfigHolder.fromProperties(props);
	}
}
