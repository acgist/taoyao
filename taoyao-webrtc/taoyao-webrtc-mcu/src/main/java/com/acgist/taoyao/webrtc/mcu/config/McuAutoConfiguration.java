package com.acgist.taoyao.webrtc.mcu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MCU自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.webrtc", name = "framework", havingValue = "MCU", matchIfMissing = false)
public class McuAutoConfiguration {

}
