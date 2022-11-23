package com.acgist.taoyao.webrtc.mesh.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.webrtc.mesh.listener.MediaSubscribeListener;

/**
 * Mesh自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.webrtc", name = "model", havingValue = "MESH", matchIfMissing = false)
public class MeshAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MediaSubscribeListener mediaSubscribeListener() {
		return new MediaSubscribeListener();
	}
	
}
