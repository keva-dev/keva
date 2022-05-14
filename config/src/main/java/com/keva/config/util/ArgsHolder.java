package com.keva.config.util;

import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToString
public class ArgsHolder {
    private final Map<String, String> values = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

    public void addFlag(String name) {
        flags.add(name);
    }

    @SuppressWarnings("ReturnOfNull")
    public String getFlag(String[] names) {
        for (String name : names) {
            if (values.containsKey(name)) {
                return "true".equalsIgnoreCase(values.get(name)) ? "true" : "false";
            }
            if (flags.contains(name)) {
                return "true";
            }
        }
        return null;
    }

    public void addArgVal(String name, String value) {
        values.put(name, value);
    }

    @SuppressWarnings("ReturnOfNull")
    public String getArgVal(String[] names) {
        for (String name : names) {
            if (values.containsKey(name)) {
                return values.get(name);
            }
        }
        return null;
    }
}
