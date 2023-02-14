package com.acgist.taoyao.signal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;
import com.acgist.taoyao.signal.media.MediaClientManager;
import com.acgist.taoyao.signal.service.SecurityService;
import com.acgist.taoyao.signal.service.impl.SecurityServiceImpl;

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
	public SecurityService securityService() {
		return new SecurityServiceImpl();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	
	@Bean
	@Autowired
	public CommandLineRunner mediaCommandLineRunner(MediaClientManager mediaClientManager) {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				mediaClientManager.init();
			}
		};
	}
	
}
