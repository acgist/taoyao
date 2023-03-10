package com.acgist.taoyao.boot.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * MVC自动配置
 * 
 * TODO：ErrorPageRegistrar拦截400错误https://localhost:8888/?\%3E%3C
 * 
 * @author acgist
 */
@Slf4j
@AutoConfiguration
public class WebMvcConfigurerAutoConfiguration implements WebMvcConfigurer {
	
	private final ApplicationContext applicationContext;
	
	public WebMvcConfigurerAutoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
	
    @Override
	public void addInterceptors(InterceptorRegistry registry) {
		this.applicationContext.getBeansOfType(InterceptorAdapter.class).entrySet().stream()
		.sorted((a, z) -> a.getValue().compareTo(z.getValue()))
		.forEach(entry -> {
			final InterceptorAdapter value = entry.getValue();
			if(log.isDebugEnabled()) {
			    log.debug("注册MVC拦截器：{} - {}", String.format("%-32s", entry.getKey()), value.name());
			}
			registry.addInterceptor(value).addPathPatterns(value.pathPattern());
		});
	}
    
//  @Override
//  public void addCorsMappings(CorsRegistry registry) {
//      WebMvcConfigurer.super.addCorsMappings(registry);
//  }
    
}
