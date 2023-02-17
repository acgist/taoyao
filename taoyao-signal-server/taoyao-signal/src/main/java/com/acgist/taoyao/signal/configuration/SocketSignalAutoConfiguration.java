package com.acgist.taoyao.signal.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.boot.property.SocketProperties;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.client.socket.SocketSignal;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

/**
 * Socket信令自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.socket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocketSignalAutoConfiguration {

	@Bean
	@Autowired
	@ConditionalOnMissingBean
	public SocketSignal socketSignal(
		ClientManager clientManager,
		ProtocolManager protocolManager,
		SocketProperties socketProperties,
		PlatformErrorProtocol platformErrorProtocol
	) {
		return new SocketSignal(clientManager, protocolManager, socketProperties, platformErrorProtocol);
	}
	
	@Bean
	@Autowired
	@ConditionalOnBean(SocketSignal.class)
	public CommandLineRunner socketSignalCommandLineRunner(SocketSignal socketSignal) {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				socketSignal.init();
			}
		};
	}
	
}
