package com.jinyframework.keva.server.command;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public interface CommandService {
    static ArrayList<String> parseTokens(String line) {
        return new ArrayList<>(Arrays.asList(line.split(" ")));
    }

    Object handleCommand(PrintWriter socketOut, String line);
}
