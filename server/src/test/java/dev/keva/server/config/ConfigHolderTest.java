package dev.keva.server.config;

import dev.keva.server.util.ArgsHolder;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHolderTest {
    @Test
    void defaultConfig() {
        val def = ConfigHolder.makeDefaultConfig();
        assertTrue(def.getSnapshotEnabled());
        assertEquals("localhost", def.getHostname());
        assertEquals(6767, def.getPort());
        assertEquals("./", def.getSnapshotLocation());
        assertEquals(64, def.getHeapSize());
    }

    @Test
    void fromPropsDefault() {
        val def = ConfigHolder.makeDefaultConfig();
        val defFromProps = ConfigHolder.fromProperties(new Properties());
        assertEquals(def, defFromProps);
    }

    @Test
    void fromProperties() {
        val props = new Properties();
        props.setProperty("hostname", "host");
        props.setProperty("port", "123123");
        props.setProperty("snapshot_enabled", "false");
        props.setProperty("snapshot_location", "./snap/");
        props.setProperty("heap_size", "123");

        val configHolder = ConfigHolder.fromProperties(props);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertFalse(configHolder.getSnapshotEnabled());
        assertEquals("./snap/", configHolder.getSnapshotLocation());
        assertEquals(123, configHolder.getHeapSize());
    }

    @Test
    void fromEmptyArgs() {
        val emptyArgs = new ArgsHolder();
        val emptyConfig = ConfigHolder.fromArgs(emptyArgs);
        assertNull(emptyConfig.getHostname());
        assertNull(emptyConfig.getPort());
        assertNull(emptyConfig.getHeapSize());
        assertNull(emptyConfig.getSnapshotLocation());
        assertNull(emptyConfig.getSnapshotEnabled());
    }

    @Test
    void fromArgs() {
        val argsHolder = new ArgsHolder();
        argsHolder.addArgVal("h", "host");
        argsHolder.addArgVal("p", "123123");
        argsHolder.addArgVal("ht", "123");
        argsHolder.addArgVal("sl", "./snap/");
        argsHolder.addArgVal("hs", "123");
        argsHolder.addFlag("hb");
        argsHolder.addFlag("ss");

        val configHolder = ConfigHolder.fromArgs(argsHolder);
        assertEquals("host", configHolder.getHostname());
        assertEquals(123123, configHolder.getPort());
        assertTrue(configHolder.getSnapshotEnabled());
        assertEquals("./snap/", configHolder.getSnapshotLocation());
        assertEquals(123, configHolder.getHeapSize());
    }

    @Test
    void merge() {
        val argsHolder = new ArgsHolder();
        argsHolder.addArgVal("h", "host");
        argsHolder.addArgVal("p", "123123");
        argsHolder.addFlag("hb");
        val fromArgs = ConfigHolder.fromArgs(argsHolder);

        val baseConfig = ConfigHolder.builder().build();
        final ConfigHolder merge = baseConfig.merge(fromArgs);
        assertEquals("host", merge.getHostname());
        assertEquals(123123, merge.getPort());
    }

}
