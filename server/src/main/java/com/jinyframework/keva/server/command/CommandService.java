package com.jinyframework.keva.server.command;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Arrays;

public interface CommandService {
    static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    Object handleCommand(ChannelHandlerContext ctx, String line);
}
