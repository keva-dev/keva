package dev.keva.server.config;

import dev.keva.server.config.util.ArgsHolder;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KevaConfigTest {
    @Test
    void defaultConfig() {
        val def = KevaConfig.ofDefaults();
        assertTrue(def.getPersistence());
        assertEquals("localhost", def.getHostname());
        assertEquals(6379, def.getPort());
        assertEquals("./", def.getWorkDirectory());
    }

    @Test
    void fromPropsDefault() {
        val def = KevaConfig.ofDefaults();
        val defFromProps = ConfigLoader.fromProperties(new Properties(), KevaConfig.class);
        assertEquals(def, defFromProps);
    }

    @Test
    void fromProperties() {
        val props = new Properties();
        props.setProperty("hostname", "host");
        props.setProperty("port", "123123");
        props.setProperty("persistence", "false");
        props.setProperty("work_directory", "./snap/");

        val configHolder = ConfigLoader.fromProperties(props, KevaConfig.class);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertFalse(configHolder.getPersistence());
        assertEquals("./snap/", configHolder.getWorkDirectory());
    }

    @Test
    void fromEmptyArgs() {
        val emptyArgs = new ArgsHolder();
        val emptyConfig = ConfigLoader.fromArgs(emptyArgs, KevaConfig.class);
        assertNull(emptyConfig.getHostname());
        assertNull(emptyConfig.getPort());
        assertNull(emptyConfig.getWorkDirectory());
        assertNull(emptyConfig.getPersistence());
    }

    @Test
    void fromArgs() {
        val argsHolder = new ArgsHolder();
        argsHolder.addArgVal("h", "host");
        argsHolder.addArgVal("p", "123123");
        argsHolder.addArgVal("ht", "123");
        argsHolder.addArgVal("dir", "./snap/");
        argsHolder.addFlag("hb");
        argsHolder.addFlag("ps");

        val configHolder = ConfigLoader.fromArgs(argsHolder, KevaConfig.class);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertTrue(configHolder.getPersistence());
        assertEquals("./snap/", configHolder.getWorkDirectory());
    }

    @Test
    void merge() {
        val argsHolder = new ArgsHolder();
        argsHolder.addArgVal("h", "host");
        argsHolder.addArgVal("p", "123123");
        argsHolder.addFlag("hb");
        val fromArgs = ConfigLoader.fromArgs(argsHolder, KevaConfig.class);

        val baseConfig = KevaConfig.builder().build();
        ConfigLoader.merge(baseConfig, fromArgs);
        assertEquals("host", baseConfig.getHostname());
        assertEquals(123123, baseConfig.getPort());
    }
}
