package dev.keva.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgsParserTest {

    @Test
    void parse() {
        String[] args = {
                "-p", "123123", "-a", "-b"
        };
        ArgsHolder parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag("a"));
        assertEquals("true", parse.getFlag("b"));
        assertEquals("123123", parse.getArgVal("p"));

        args = new String[]{
                "-a", "-b", "c"
        };
        parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag("a"));
        assertEquals("c", parse.getArgVal("b"));
    }
}
