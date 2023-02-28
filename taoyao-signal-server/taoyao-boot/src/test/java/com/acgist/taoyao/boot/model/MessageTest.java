package com.acgist.taoyao.boot.model;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.utils.JSONUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageTest {

    @Test
    public void testJSON() {
        final Message message = Message.builder()
            .code("0000")
            .message("acgist")
            .body(Map.of("1", "2"))
            .build();
        final String json = JSONUtils.toJSON(message);
        log.info("{}", json);
    }
    
}
