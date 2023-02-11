package com.acgist.taoyao.signal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;
import com.acgist.taoyao.signal.mediasoup.MediasoupClientManager;
import com.acgist.taoyao.signal.protocol.media.MediaRebootProtocol;
import com.acgist.taoyao.signal.protocol.media.MediaShutdownProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformScriptProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;
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

	@Autowired
	private MediasoupClientManager mediasoupClientManager;
	
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
	@ConditionalOnMissingBean
	public SecurityService securityService() {
		return new SecurityServiceImpl();
	}
	
	@Bean
	public CommandLineRunner mediasoupCommandLineRunner() {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				SignalAutoConfiguration.this.mediasoupClientManager.init();
			}
		};
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public MediaRebootProtocol mediaRebootProtocol() {
		return new MediaRebootProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public MediaShutdownProtocol mediaShutdownProtocol() {
		return new MediaShutdownProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public SystemRebootProtocol systemRebootProtocol() {
		return new SystemRebootProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public SystemShutdownProtocol systemShutdownProtocol() {
		return new SystemShutdownProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public PlatformRebootProtocol platformRebootProtocol() {
		return new PlatformRebootProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public PlatformScriptProtocol platformScriptProtocol() {
		return new PlatformScriptProtocol();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public PlatformShutdownProtocol platformShutdownProtocol() {
		return new PlatformShutdownProtocol();
	}
	
}
