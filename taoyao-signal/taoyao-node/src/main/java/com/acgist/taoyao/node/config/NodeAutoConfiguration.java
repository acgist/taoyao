package com.acgist.taoyao.node.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 集群自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.node", name = "enabled", havingValue = "true", matchIfMissing = false)
public class NodeAutoConfiguration {
	
//	@Bean
//	@ConditionalOnMissingBean
//	public MediaPublishListener mediaPublishListener() {
//		return new MediaPublishListener();
//	}
//
//	@Bean
//	@ConditionalOnMissingBean
//	public MediaSubscribeListener mediaSubscribeListener() {
//		return new MediaSubscribeListener();
//	}
//	
//	@Bean
//	@ConditionalOnMissingBean
//	public MediaOfferListener mediaOfferListener() {
//		return new MediaOfferListener();
//	}
//	
//	@Bean
//	@ConditionalOnMissingBean
//	public MediaAnswerListener mediaAnswerListener() {
//		return new MediaAnswerListener();
//	}
//	
//	@Bean
//	@ConditionalOnMissingBean
//	public MediaCandidateListener mediaCandidateListener() {
//		return new MediaCandidateListener();
//	}
	
}
