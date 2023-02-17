package com.acgist.taoyao.boot.utils;

import org.junit.jupiter.api.Test;

public class ScriptUtilsTest {

    @Test
    void test() {
        ScriptUtils.execute("ls");
        ScriptUtils.execute("netstat -ano");
    }
    
}
