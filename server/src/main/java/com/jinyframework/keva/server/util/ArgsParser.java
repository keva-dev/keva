package com.jinyframework.keva.server.util;

import lombok.val;

import java.util.*;

public final class ArgsParser {
    private ArgsParser() {
    }

    public static Map<String, String> parse(String[] args, Set<String> options) {
        return parse(new ArrayList<>(Arrays.asList(args)), options);
    }

    public static Map<String, String> parse(List<String> args, Set<String> options) {
        val config = new HashMap<String, String>();
        for (int i = 0; i < args.size(); i++) {
            val token = args.get(i);
            if (token.startsWith("-")) {
                val opt = token.substring(1);
                if (options.contains(opt)) {
                    config.put(opt, args.get(i + 1));
                }
            }
        }
        return config;
    }
}
