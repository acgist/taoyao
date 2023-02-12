package com.acgist.taoyao.signal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.acgist.taoyao.boot.property.SocketProperties;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.socket.SocketSignal;
import com.acgist.taoyao.signal.client.websocket.WebSocketSignal;
import com.acgist.taoyao.signal.media.MediaClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
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
	private MediaClientManager mediaClientManager;
	
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
	public CommandLineRunner mediaCommandLineRunner() {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				SignalAutoConfiguration.this.mediaClientManager.init();
			}
		};
	}
	
	@Bean
	@Autowired
	@ConditionalOnProperty(prefix = "taoyao.socket", name = "enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean
	public SocketSignal socketSignal(
		ClientManager clientManager,
		ProtocolManager protocolManager,
		SocketProperties socketProperties
	) {
		return new SocketSignal(clientManager, protocolManager, socketProperties);
	}
	
	@Bean
	@Autowired
	@ConditionalOnProperty(prefix = "taoyao.socket", name = "enabled", havingValue = "true", matchIfMissing = true)
	public CommandLineRunner socketSignalCommandLineRunner(
		SocketSignal socketSignal
	) {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				socketSignal.listen();
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
