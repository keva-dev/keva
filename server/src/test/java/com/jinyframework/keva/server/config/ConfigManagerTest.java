package com.jinyframework.keva.server.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;

import static com.jinyframework.keva.server.config.ConfigHolder.makeDefaultConfig;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ConfigManagerTest {
    @Test
    void loadConfigArgs() throws Exception {
        String[] args = {
                ""
        };
        ConfigHolder configDef = ConfigManager.loadConfig(args);
        val def = makeDefaultConfig();
        assertEquals(def, configDef);
        args = new String[]{
                "-p", "123123"
        };
        ConfigHolder configOverriden = ConfigManager.loadConfig(args);
        assertEquals(123123, configOverriden.getPort());
    }

    @Test
    void loadConfigFromFile() throws Exception {
        val testPropPath = "./keva.test.properties";
        val conf = new File(testPropPath);
        if (conf.exists()) {
            val deleted = conf.delete();
            if (deleted) {
                val newFile = conf.createNewFile();
                if (!newFile) {
                    fail("Failed to create config file");
                }
            }
        }

        try (val fileWriter = new FileWriter(testPropPath)) {
            fileWriter.write("port = 123123\nheartbeat_enabled = false");
            fileWriter.flush();
        }

        final String[] args = {
                "-f", testPropPath
        };
        final ConfigHolder configOverriden = ConfigManager.loadConfig(args);
        assertEquals(123123, configOverriden.getPort());
    }

    @Test
    void loadConfigOverride() throws Exception {
        val testPropPath = "./keva.test.properties";
        val conf = new File(testPropPath);
        if (conf.exists()) {
            val deleted = conf.delete();
            if (deleted) {
                val newFile = conf.createNewFile();
                if (!newFile) {
                    fail("Failed to create config file");
                }
            }
        }

        try (val fileWriter = new FileWriter(testPropPath)) {
            fileWriter.write("port = 123123\nheartbeat_enabled = false");
            fileWriter.flush();
        }

        final String[] args = {
                "-f", testPropPath, "-p", "123"
        };
        final ConfigHolder configOverriden = ConfigManager.loadConfig(args);
        assertEquals(123, configOverriden.getPort());
    }
}
