package com.acgist.taoyao.webrtc.sfu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * SFU自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.webrtc", name = "model", havingValue = "SFU", matchIfMissing = true)
public class SfuAutoConfiguration {

}
