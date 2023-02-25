package com.acgist.taoyao.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.interceptor.SecurityInterceptor;
import com.acgist.taoyao.interceptor.SlowInterceptor;
import com.acgist.taoyao.signal.service.SecurityService;
import com.acgist.taoyao.signal.service.UsernamePasswordService;
import com.acgist.taoyao.signal.service.impl.SecurityServiceImpl;

/**
 * 自动配置
 * 
 * @author acgist
 */
@AutoConfiguration
public class TaoyaoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SlowInterceptor slowInterceptor(TaoyaoProperties taoyaoProperties) {
		return new SlowInterceptor(taoyaoProperties);
	}
	
    @Bean
    @ConditionalOnMissingBean
    public SecurityService securityService(
        SecurityProperties securityProperties,
        @Autowired(required = false) UsernamePasswordService usernamePasswordService
    ) {
        return new SecurityServiceImpl(securityProperties, usernamePasswordService);
    }
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.security", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public SecurityInterceptor securityInterceptor(SecurityService securityService, SecurityProperties securityProperties) {
		return new SecurityInterceptor(securityService, securityProperties);
	}
	
}
