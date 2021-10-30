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
        assertEquals(6767, def.getPort());
        assertEquals("./", def.getWorkDirectory());
        assertEquals(64, def.getHeapSize());
    }

    @Test
    void fromPropsDefault() {
        val def = KevaConfig.ofDefaults();
        val defFromProps = KevaConfig.fromProperties(new Properties());
        assertEquals(def, defFromProps);
    }

    @Test
    void fromProperties() {
        val props = new Properties();
        props.setProperty("hostname", "host");
        props.setProperty("port", "123123");
        props.setProperty("persistence", "false");
        props.setProperty("work_directory", "./snap/");
        props.setProperty("heap_size", "123");

        val configHolder = KevaConfig.fromProperties(props);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertFalse(configHolder.getPersistence());
        assertEquals("./snap/", configHolder.getWorkDirectory());
        assertEquals(123, configHolder.getHeapSize());
    }

    @Test
    void fromEmptyArgs() {
        val emptyArgs = new ArgsHolder();
        val emptyConfig = KevaConfig.fromArgs(emptyArgs);
        assertNull(emptyConfig.getHostname());
        assertNull(emptyConfig.getPort());
        assertNull(emptyConfig.getHeapSize());
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
        argsHolder.addArgVal("hs", "123");
        argsHolder.addFlag("hb");
        argsHolder.addFlag("ps");

        val configHolder = KevaConfig.fromArgs(argsHolder);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertTrue(configHolder.getPersistence());
        assertEquals("./snap/", configHolder.getWorkDirectory());
        assertEquals(123, configHolder.getHeapSize());
    }

    @Test
    void merge() {
        val argsHolder = new ArgsHolder();
        argsHolder.addArgVal("h", "host");
        argsHolder.addArgVal("p", "123123");
        argsHolder.addFlag("hb");
        val fromArgs = KevaConfig.fromArgs(argsHolder);

        val baseConfig = KevaConfig.builder().build();
        val merge = baseConfig.merge(fromArgs);
        assertEquals("host", merge.getHostname());
        assertEquals(123123, merge.getPort());
    }

}
