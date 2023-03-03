package com.acgist.taoyao.signal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.main.TaoyaoApplication;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TaoyaoTest(classes = TaoyaoApplication.class)
public class PlatformErrorProtocolTest {
    
    @Autowired
    private PlatformErrorProtocol platformErrorProtocol;
    
    @Test
    public void testException() {
        log.info("{}", this.platformErrorProtocol.build(MessageCodeException.of("自定义")));
        log.info("{}", this.platformErrorProtocol.build(new NullPointerException("空指针")));
    }

}
