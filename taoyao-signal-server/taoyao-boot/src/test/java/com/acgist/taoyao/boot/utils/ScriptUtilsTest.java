package com.acgist.taoyao.boot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScriptUtilsTest {

    @Test
    void test() throws InterruptedException {
        assertNotEquals(0, ScriptUtils.execute("ls").getCode());
        assertEquals(0, ScriptUtils.execute("netstat -ano").getCode());
        log.info("{}", ScriptUtils.execute("ls").getResult());
        log.info("{}", ScriptUtils.execute("netstat -ano").getResult());
    }
    
}
