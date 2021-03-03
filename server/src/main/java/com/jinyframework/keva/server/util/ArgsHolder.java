package com.jinyframework.keva.server.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArgsHolder {
    private final Map<String, String> values = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

    public void addFlag(String name) {
        flags.add(name);
    }

    public String getFlag(String name) {
        return Boolean.toString(flags.contains(name));
    }

    public void addArgVal(String name, String value) {
        values.put(name,value);
    }

    @SuppressWarnings("ReturnOfNull")
    public String getArgVal(String name) throws Exception {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ArgsHolder{" +
                "values=" + values +
                ", flags=" + flags +
                '}';
    }
}
