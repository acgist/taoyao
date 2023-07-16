package com.acgist.taoyao.boot.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.acgist.taoyao.boot.interceptor.InterceptorAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MVC自动配置
 * 
 * @author acgist
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class WebMvcConfigurerAutoConfiguration implements WebMvcConfigurer {
    
    private final ApplicationContext applicationContext;
    
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
    
}
