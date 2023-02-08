package com.acgist.taoyao.node.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.node.listener.platform.NodeShutdownListener;

/**
 * 集群自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.node", name = "enabled", havingValue = "true", matchIfMissing = false)
public class NodeAutoConfiguration {
	
	@Bean
	@ConditionalOnMissingBean
	public NodeShutdownListener nodeShutdownListener() {
		return new NodeShutdownListener();
	}
	
}
