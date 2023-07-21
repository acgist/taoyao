package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;

/**
 * WebSocket信令自动配置
 * 
 * @author acgist
 */
@EnableWebSocket
@AutoConfiguration
public class WebSocketSignalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebSocketSignal webSocketSignal() {
        return new WebSocketSignal();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
}
