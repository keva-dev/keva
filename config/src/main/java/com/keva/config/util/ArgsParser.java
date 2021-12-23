package com.keva.config.util;

public final class ArgsParser {
    public static ArgsHolder parse(String[] args) {
        ArgsHolder holder = new ArgsHolder();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token.startsWith("--")) {
                String name = token.substring(2);
                if (i >= args.length - 1) {
                    holder.addFlag(name);
                } else if (args[i + 1].startsWith("--")) {
                    holder.addFlag(name);
                } else if (!args[i + 1].isEmpty()) {
                    holder.addArgVal(name, args[i + 1]);
                }
            }
        }
        return holder;
    }
}
