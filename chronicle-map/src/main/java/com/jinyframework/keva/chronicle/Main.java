package com.jinyframework.keva.chronicle;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class Main {
    public static void main(String[] args) {
        @Cleanup ChronicleMap<String, String> chronicleMap = ChronicleMapBuilder
                .of(String.class, String.class)
                .name("demo")
                .entries(100).averageKey("this-is-test-key").averageValue("this-is-average-val")
                .create();

        chronicleMap.put("test", "Hello World");

        log.info("Test is: " + chronicleMap.get("test"));
        log.info("Undefined is: " + chronicleMap.get("test1"));

        chronicleMap.remove("test");

        log.info("Test is: " + chronicleMap.get("test"));
    }
}
