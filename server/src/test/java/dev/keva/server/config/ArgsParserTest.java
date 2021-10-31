package dev.keva.server.config;

import dev.keva.server.config.util.ArgsParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        args = new String[]{
                "-a", "-b", "false"
        };
        parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag("a"));
        assertEquals("false", parse.getFlag("b"));

        args = new String[]{
                "-a", "aa", "-b", "notTrue"
        };
        parse = ArgsParser.parse(args);
        assertEquals("aa", parse.getArgVal("a"));
        assertEquals("false", parse.getFlag("b"));
        assertNull(parse.getFlag("c"));
        assertNull(parse.getArgVal("c"));
    }
}
