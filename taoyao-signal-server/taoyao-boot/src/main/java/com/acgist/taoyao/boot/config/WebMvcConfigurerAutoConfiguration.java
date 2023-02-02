package com.acgist.taoyao.boot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;

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
	private ApplicationContext context;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		this.context.getBeansOfType(InterceptorAdapter.class).entrySet().stream()
		.sorted((a, z) -> a.getValue().compareTo(z.getValue()))
		.forEach(entry -> {
			final InterceptorAdapter value = entry.getValue();
			log.info("加载拦截器：{}-{}", entry.getKey(), value.name());
			registry.addInterceptor(value).addPathPatterns(value.pathPattern());
		});
	}
	
}
