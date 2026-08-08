package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令自动配置
 * 
 * @author acgist
 */
@Slf4j
@EnableWebSocket
@AutoConfiguration
public class WebSocketSignalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebSocketSignal webSocketSignal() {
        log.info("注册WebSocket信令");
        return new WebSocketSignal();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        final ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(60L * 1000);
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        return container;
    }
    
}
