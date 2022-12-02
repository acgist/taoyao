package com.acgist.taoyao.webrtc.moon.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MOON自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.webrtc", name = "framework", havingValue = "MOON", matchIfMissing = false)
public class MoonAutoConfiguration {

}
