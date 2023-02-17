package com.acgist.taoyao.signal.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acgist.taoyao.signal.protocol.media.MediaRebootProtocol;
import com.acgist.taoyao.signal.protocol.media.MediaShutdownProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformRebootProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformScriptProtocol;
import com.acgist.taoyao.signal.protocol.platform.PlatformShutdownProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemRebootProtocol;
import com.acgist.taoyao.signal.protocol.system.SystemShutdownProtocol;

/**
 * 脚本自动配置
 * 
 * @author acgist
 */
@Configuration
@ConditionalOnProperty(prefix = "taoyao.script", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScriptAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MediaRebootProtocol mediaRebootProtocol() {
		return new MediaRebootProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public MediaShutdownProtocol mediaShutdownProtocol() {
		return new MediaShutdownProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SystemRebootProtocol systemRebootProtocol() {
		return new SystemRebootProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SystemShutdownProtocol systemShutdownProtocol() {
		return new SystemShutdownProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public PlatformScriptProtocol platformScriptProtocol() {
		return new PlatformScriptProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public PlatformRebootProtocol platformRebootProtocol() {
		return new PlatformRebootProtocol();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public PlatformShutdownProtocol platformShutdownProtocol() {
		return new PlatformShutdownProtocol();
	}
	
}
