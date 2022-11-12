package com.acgist.taoyao.signal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.session.websocket.WebSocketSignal;

/**
 * 信令配置
 * 
 * @author acgist
 */
@Configuration
@EnableWebSocket
public class SignalAutoConfiguration {

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
