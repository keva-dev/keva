package com.keva.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class ConfigLoaderTest {
    @Test
    void loadConfigArgs() throws Exception {
        String[] args = {
                ""
        };
        KevaConfig configDef = ConfigLoader.loadConfig(args, KevaConfig.class);
        val def = KevaConfig.ofDefaults();
        assertEquals(def, configDef);
        args = new String[]{
                "-p", "123123"
        };
        KevaConfig configOverridden = ConfigLoader.loadConfig(args, KevaConfig.class);
        assertEquals(123123, configOverridden.getPort());
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
            fileWriter.write("port = 123123");
            fileWriter.flush();
        }

        final String[] args = {
                "-f", testPropPath
        };
        val configOverridden = ConfigLoader.loadConfig(args, KevaConfig.class);
        assertEquals(123123, configOverridden.getPort());
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
            fileWriter.write("port = 123123");
            fileWriter.flush();
        }

        final String[] args = {
                "-f", testPropPath, "-p", "123"
        };
        final KevaConfig configOverridden = ConfigLoader.loadConfig(args, KevaConfig.class);
        assertEquals(123, configOverridden.getPort());
    }
}
