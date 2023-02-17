package com.acgist.taoyao.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.interceptor.SecurityInterceptor;
import com.acgist.taoyao.interceptor.SlowInterceptor;
import com.acgist.taoyao.signal.service.SecurityService;
import com.acgist.taoyao.signal.service.impl.SecurityServiceImpl;

/**
 * 自动配置
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
    public SecurityService securityService() {
        return new SecurityServiceImpl();
    }
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.security", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public SecurityInterceptor securityInterceptor() {
		return new SecurityInterceptor();
	}
	
}
