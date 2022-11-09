package com.acgist.taoyao.signal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.session.SessionManager;
import com.acgist.taoyao.signal.session.websocket.TaoyaoWebSocket;

/**
 * 信令配置
 * 
 * @author acgist
 */
@Configuration
public class SignalAutoConfiguration {

	@Autowired
	private ProtocolManager eventManager;
	@Autowired
	private SessionManager sessionManager;
	
	@Autowired
	public TaoyaoWebSocket taoyaoWebSocket() {
		return new TaoyaoWebSocket(this.eventManager, this.sessionManager);
	}
	
	@Bean
	@ConditionalOnMissingBean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	
}
