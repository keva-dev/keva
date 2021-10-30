package dev.keva.server.config;

import dev.keva.server.config.util.ArgsParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArgsParserTest {

    @Test
    void parse() {
        String[] args = {
                "-p", "123123", "-a", "-b"
        };
        var parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag("a"));
        assertEquals("true", parse.getFlag("b"));
        assertEquals("123123", parse.getArgVal("p"));

        args = new String[]{
                "-a", "-b", "false"
        };
        parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag("a"));
        assertEquals("false", parse.getArgVal("b"));
    }
}
