package com.acgist.taoyao.boot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtilsTest {

    @Test
    void test() {
        assertEquals("0B", FileUtils.formatSize(0L));
        assertEquals("0B", FileUtils.formatSize(-10L));
        assertEquals("1.00KB", FileUtils.formatSize(1024L));
        log.info("{}", FileUtils.formatSize(1025L));
        log.info("{}", FileUtils.formatSize(2025L));
        log.info("{}", FileUtils.formatSize(Long.MAX_VALUE));
    }
    
}
