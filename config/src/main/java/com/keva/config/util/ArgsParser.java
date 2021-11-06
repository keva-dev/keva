package com.keva.config.util;

import lombok.val;

public final class ArgsParser {
    public static ArgsHolder parse(String[] args) {
        val holder = new ArgsHolder();
        for (int i = 0; i < args.length; i++) {
            val token = args[i];
            if (token.startsWith("-")) {
                val name = token.substring(1);
                if (i >= args.length - 1) {
                    holder.addFlag(name);
                } else if (args[i + 1].startsWith("-")) {
                    holder.addFlag(name);
                } else if (!args[i + 1].isEmpty()) {
                    holder.addArgVal(name, args[i + 1]);
                }
            }
        }
        return holder;
    }
}
