package com.acgist.taoyao.signal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;
import com.acgist.taoyao.signal.listener.platform.ScriptListener;
import com.acgist.taoyao.signal.protocol.platform.ScriptProtocol;
import com.acgist.taoyao.signal.protocol.platform.ShutdownProtocol;

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
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.signal.platform.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public ScriptProtocol scriptProtocol() {
		return new ScriptProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.signal.platform.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public ScriptListener scriptListener() {
		return new ScriptListener();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.signal.platform.shutdown", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public ShutdownProtocol shutdownProtocol() {
		return new ShutdownProtocol();
	}
	
}
