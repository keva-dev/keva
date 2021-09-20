package com.jinyframework.keva.proxy.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;

public interface CommandService {
    static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    static String parseLine(List<String> tokens) {
        return String.join(" ", tokens);
    }

    void handleCommand(ChannelHandlerContext ctx, String line);
}
