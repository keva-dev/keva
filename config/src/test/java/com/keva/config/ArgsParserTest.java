package com.keva.config;

import com.keva.config.util.ArgsHolder;
import com.keva.config.util.ArgsParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArgsParserTest {

    @Test
    void parse() {
        String[] args = {
                "--p", "123123", "--a", "--b"
        };
        ArgsHolder parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag(new String[]{"a"}));
        assertEquals("true", parse.getFlag(new String[]{"b"}));
        assertEquals("123123", parse.getArgVal(new String[]{"p"}));

        args = new String[]{
                "--a", "--b", "false"
        };
        parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag(new String[]{"a"}));
        assertEquals("false", parse.getArgVal(new String[]{"b"}));

        args = new String[]{
                "--a", "--b", "false"
        };
        parse = ArgsParser.parse(args);
        assertEquals("true", parse.getFlag(new String[]{"a"}));
        assertEquals("false", parse.getFlag(new String[]{"b"}));

        args = new String[]{
                "--a", "aa", "--b", "notTrue"
        };
        parse = ArgsParser.parse(args);
        assertEquals("aa", parse.getArgVal(new String[]{"a"}));
        assertEquals("false", parse.getFlag(new String[]{"b"}));
        assertNull(parse.getFlag(new String[]{"c"}));
        assertNull(parse.getArgVal(new String[]{"c"}));
    }
}
