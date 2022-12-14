package com.acgist.taoyao.webrtc.mesh.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.webrtc.mesh.listener.MediaAnswerListener;
import com.acgist.taoyao.webrtc.mesh.listener.MediaCandidateListener;
import com.acgist.taoyao.webrtc.mesh.listener.MediaOfferListener;
import com.acgist.taoyao.webrtc.mesh.listener.MediaPublishListener;
import com.acgist.taoyao.webrtc.mesh.listener.MediaSubscribeListener;

/**
 * Mesh自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.webrtc", name = "framework", havingValue = "MESH", matchIfMissing = false)
public class MeshAutoConfiguration {
	
	@Bean
	@ConditionalOnMissingBean
	public MediaPublishListener mediaPublishListener() {
		return new MediaPublishListener();
	}

	@Bean
	@ConditionalOnMissingBean
	public MediaSubscribeListener mediaSubscribeListener() {
		return new MediaSubscribeListener();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public MediaOfferListener mediaOfferListener() {
		return new MediaOfferListener();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public MediaAnswerListener mediaAnswerListener() {
		return new MediaAnswerListener();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public MediaCandidateListener mediaCandidateListener() {
		return new MediaCandidateListener();
	}
	
}
