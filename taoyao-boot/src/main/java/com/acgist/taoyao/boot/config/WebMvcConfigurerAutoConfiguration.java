package com.acgist.taoyao.boot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.acgist.taoyao.boot.interceptor.SecurityInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * MVC配置
 * 
 * @author acgist
 */
@Slf4j
@Configuration
public class WebMvcConfigurerAutoConfiguration implements WebMvcConfigurer {
	
	@Autowired
	private SecurityInterceptor securityInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		log.info("加载拦截器：securityInterceptor");
		registry.addInterceptor(this.securityInterceptor).addPathPatterns("/**");
	}
	
}
