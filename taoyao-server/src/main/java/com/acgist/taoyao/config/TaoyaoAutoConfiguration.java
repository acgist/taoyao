package com.acgist.taoyao.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.interceptor.SecurityInterceptor;
import com.acgist.taoyao.interceptor.SlowInterceptor;

/**
 * 配置
 * 
 * @author acgist
 */
@Configuration
public class TaoyaoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SlowInterceptor slowInterceptor() {
		return new SlowInterceptor();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SecurityInterceptor securityInterceptor() {
		return new SecurityInterceptor();
	}
	
}
