package com.jinyframework.keva.engine;

import java.io.IOException;

public class AppTest {
    public static void main(String[] args) throws IOException {
        ChronicleStringKevaMap chronicleStringKevaMap = new ChronicleStringKevaMap("");
        chronicleStringKevaMap.put("Hello", "World");
        System.out.println(chronicleStringKevaMap.get("Hello"));
    }
}
