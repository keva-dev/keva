package com.jinyframework.keva.server.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.jinyframework.keva.proxy.util.HashUtil;

class HashUtilTest {

    @Test
    void hash() {
        String key = "keva";
        long hashValue = 3455670683L;
        assertEquals(hashValue, HashUtil.hash(key));
    }
}
