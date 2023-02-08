package com.acgist.taoyao.boot.property;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 脚本配置
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "taoyao.script")
public class ScriptProperties {

	/**
	 * 系统类型
	 * 
	 * @author acgist
	 */
	public enum SystemType {
		
		/**
		 * Mac
		 */
		MAC("Mac OS", "Mac OS X"),
		/**
		 * Linux
		 */
		LINUX("Linux"),
		/**
		 * Windows
		 */
		WINDOWS("Windows XP", "Windows Vista", "Windows 7", "Windows 10", "Windows 11"),
		/**
		 * Android
		 */
		ANDROID("Android");
		
		/**
		 * 系统名称
		 */
		private final String[] osNames;

		/**
		 * @param osNames 系统名称
		 */
		private SystemType(String ... osNames) {
			this.osNames = osNames;
		}

		/**
		 * @param osName 系统名称
		 * 
		 * @return 系统类型
		 */
		private static final SystemType of(String osName) {
			final SystemType[] values = SystemType.values();
			for (SystemType value : values) {
				if(ArrayUtils.contains(value.osNames, osName)) {
					return value;
				}
			}
			return SystemType.LINUX;
		}
		
	}
	
	/**
	 * Linux脚本
	 */
	private Map<String, String> linux;
	/**
	 * Windows脚本
	 */
	private Map<String, String> windows;
	/**
	 * 重启媒体服务
	 */
	private String mediaReboot;
	/**
	 * 关闭媒体服务
	 */
	private String mediaShutdown;
	/**
	 * 重启系统
	 */
	private String systemReboot;
	/**
	 * 关闭系统
	 */
	private String systemShutdown;
	/**
	 * 重启平台
	 */
	private String platformReboot;
	/**
	 * 关闭平台
	 */
	private String platformShutdown;
	
	@PostConstruct
	public void init() {
		final String osName = System.getProperty("os.name");
		final SystemType type = SystemType.of(osName);
		switch (type) {
		case LINUX -> this.set(this.linux);
		case WINDOWS -> this.set(this.windows);
		default -> log.error("没有配置系统脚本：{}", type);
		}
	}
	
	/**
	 * 配置脚本
	 * 
	 * @param map 脚本
	 */
	private void set(Map<String, String> map) {
		this.mediaReboot = map.get("media-reboot");
		this.mediaShutdown = map.get("media-shutdown");
		this.systemReboot = map.get("system-reboot");
		this.systemShutdown = map.get("system-shutdown");
		this.platformReboot = map.get("platform-reboot");
		this.platformShutdown = map.get("platform-shutdown");
	}
	
}
